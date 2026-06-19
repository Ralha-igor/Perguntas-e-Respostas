package quiz.rede;

import quiz.modelo.*;
import quiz.util.LeitorPerguntas;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Servidor {

    private static final int PORTA          = 12345;
    private static final int MAX_JOGADORES  = 2;
    private static final int TEMPO_LEITURA  = 15; // segundos antes de liberar buzzer
    private static final int TEMPO_RESPOSTA = 20; // segundos para responder após clicar

    // Canais de comunicação
    private final ObjectOutputStream[] outputs = new ObjectOutputStream[MAX_JOGADORES];
    private final ObjectInputStream[]  inputs  = new ObjectInputStream[MAX_JOGADORES];

    // Controles de buzzer (recriados a cada rodada)
    private volatile AtomicInteger  buzzerVencedor;
    private volatile CountDownLatch buzzerLatch;

    // Controles de resposta (recriados a cada tentativa)
    private volatile AtomicInteger  respostaRecebida;
    private volatile CountDownLatch respostaLatch;

    private List<Pergunta> perguntas;
    private Partida partida;

    // ─────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) throws Exception {
        new Servidor().iniciar();
    }

    public void iniciar() throws Exception {
        String ip = InetAddress.getLocalHost().getHostAddress();
        System.out.println("=== SERVIDOR QUIZ - PARCIAL 2 ===");
        System.out.println("IP: " + ip + "  Porta: " + PORTA);

        perguntas = LeitorPerguntas.carregar("recursos/perguntas.txt");
        System.out.println("Perguntas carregadas: " + perguntas.size());

        aguardarJogadores();
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void aguardarJogadores() throws Exception {
        System.out.println("\nAguardando " + MAX_JOGADORES + " jogadores na porta " + PORTA + "...");

        try (ServerSocket servidor = new ServerSocket(PORTA)) {

            for (int i = 0; i < MAX_JOGADORES; i++) {
                Socket conexao = servidor.accept();

                outputs[i] = new ObjectOutputStream(conexao.getOutputStream());
                outputs[i].flush();
                inputs[i]  = new ObjectInputStream(conexao.getInputStream());

                enviar(i, new Mensagem(Mensagem.Tipo.IDENTIFICACAO, i));
                System.out.println("Jogador " + (i + 1) + " conectado: "
                        + conexao.getInetAddress().getHostAddress());
            }

            System.out.println("\nAmbos conectados! Iniciando partida...");
            broadcast(new Mensagem(Mensagem.Tipo.AGUARDANDO_JOGADOR,
                    "Ambos conectados! Jogo iniciando..."));

            iniciarPartida();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void iniciarPartida() throws Exception {
        partida = new Partida(perguntas.size());

        while (!partida.finalizada()) {
            executarRodada(partida.getRodadaAtual());
            partida.proximaRodada();
        }

        encerrarJogo();
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void executarRodada(int indice) throws Exception {
        Pergunta pergunta = perguntas.get(indice);

        System.out.printf("%n── Rodada %d/%d: %s%n",
                indice + 1, partida.getTotalRodadas(), pergunta.getEnunciado());

        // Reseta controles de buzzer para esta rodada
        buzzerVencedor   = new AtomicInteger(-1);
        buzzerLatch      = new CountDownLatch(1);
        respostaRecebida = new AtomicInteger(Integer.MIN_VALUE);
        respostaLatch    = new CountDownLatch(1);

        // Envia pergunta — cliente bloqueia o botão de buzzer
        broadcast(new Mensagem(Mensagem.Tipo.NOVA_PERGUNTA, pergunta));

        // Threads de leitura paralela: ouvem CLICOU_BUZZER durante os 15s + 20s de espera
        Thread[] leitores = iniciarLeitoresDeBuzzer();

        // Pausa de leitura
        System.out.println("  Leitura: " + TEMPO_LEITURA + "s...");
        sleep(TEMPO_LEITURA * 1000L);

        // Libera buzzer
        broadcast(new Mensagem(Mensagem.Tipo.LIBERAR_BUZZER, null));
        System.out.println("  Buzzer liberado! Aguardando clique...");

        // Aguarda clique por até TEMPO_RESPOSTA segundos
        boolean alguemClicou = buzzerLatch.await(TEMPO_RESPOSTA, TimeUnit.SECONDS);
        pararThreads(leitores);

        if (!alguemClicou) {
            System.out.println("  Ninguém clicou — rodada encerrada.");
            broadcast(new Mensagem(Mensagem.Tipo.RESULTADO_RODADA,
                    new Object[]{-1, false, false, clonarPartida(partida)}));
            return;
        }

        int clicante  = buzzerVencedor.get();
        int adversario = 1 - clicante;
        System.out.println("  Jogador " + (clicante + 1) + " clicou primeiro!");

        // Notifica ambos
        enviar(clicante,   new Mensagem(Mensagem.Tipo.VOCE_RESPONDEU, null));
        enviar(adversario, new Mensagem(Mensagem.Tipo.BUZZER_BLOQUEADO, null));

        // Aguarda resposta do clicante
        respostaRecebida = new AtomicInteger(Integer.MIN_VALUE);
        respostaLatch    = new CountDownLatch(1);

        boolean respondeu = aguardarResposta(clicante, TEMPO_RESPOSTA);
        int resposta = respostaRecebida.get();

        if (!respondeu || resposta == Integer.MIN_VALUE) {
            // Timeout sem resposta
            System.out.println("  J" + (clicante + 1) + " não respondeu → -1 ponto.");
            partida.removerPonto(clicante);
            broadcast(new Mensagem(Mensagem.Tipo.RESULTADO_RODADA,
                    new Object[]{clicante, false, true, clonarPartida(partida)}));

            // Adversário recebe segunda chance, mas SEM risco (timeout não é erro do clicante)
            executarSegundaChance(adversario, pergunta, /*semRisco=*/true);

        } else if (pergunta.verificarResposta(resposta)) {
            System.out.println("  J" + (clicante + 1) + " ACERTOU! +1 ponto.");
            partida.adicionarPonto(clicante);
            broadcast(new Mensagem(Mensagem.Tipo.RESULTADO_RODADA,
                    new Object[]{clicante, true, false, clonarPartida(partida)}));

        } else {
            System.out.println("  J" + (clicante + 1) + " ERROU → -1 ponto.");
            partida.removerPonto(clicante);
            broadcast(new Mensagem(Mensagem.Tipo.RESULTADO_RODADA,
                    new Object[]{clicante, false, true, clonarPartida(partida)}));

            // Adversário tenta — se errar, SEM penalidade (regra da segunda chance)
            executarSegundaChance(adversario, pergunta, /*semRisco=*/true);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Segunda chance para o adversário.
     * @param semRisco  true → errar não desconta ponto
     */
    private void executarSegundaChance(int jogador, Pergunta pergunta, boolean semRisco)
            throws Exception {

        System.out.println("  Segunda chance: J" + (jogador + 1)
                + (semRisco ? " (sem risco)" : "") + "...");

        respostaRecebida = new AtomicInteger(Integer.MIN_VALUE);
        respostaLatch    = new CountDownLatch(1);

        enviar(jogador, new Mensagem(Mensagem.Tipo.SEGUNDA_CHANCE, pergunta));

        boolean respondeu = aguardarResposta(jogador, TEMPO_RESPOSTA);
        int resposta = respostaRecebida.get();

        if (!respondeu || resposta == Integer.MIN_VALUE) {
            System.out.println("  J" + (jogador + 1) + " não respondeu na segunda chance.");
            broadcast(new Mensagem(Mensagem.Tipo.RESULTADO_RODADA,
                    new Object[]{jogador, false, false, clonarPartida(partida)}));

        } else if (pergunta.verificarResposta(resposta)) {
            System.out.println("  J" + (jogador + 1) + " acertou na segunda chance! +1 ponto.");
            partida.adicionarPonto(jogador);
            broadcast(new Mensagem(Mensagem.Tipo.RESULTADO_RODADA,
                    new Object[]{jogador, true, false, clonarPartida(partida)}));

        } else {
            String penalidade = semRisco ? "sem penalidade" : "-1 ponto";
            System.out.println("  J" + (jogador + 1) + " errou na segunda chance → " + penalidade + ".");
            if (!semRisco) partida.removerPonto(jogador);
            broadcast(new Mensagem(Mensagem.Tipo.RESULTADO_RODADA,
                    new Object[]{jogador, false, !semRisco, clonarPartida(partida)}));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void encerrarJogo() throws Exception {
        int venc = partida.vencedor();
        String msg = venc == -1
                ? "Empate! J1=" + partida.getPontos(0) + " J2=" + partida.getPontos(1)
                : "Vencedor: J" + (venc + 1) + "! Placar: J1="
                        + partida.getPontos(0) + " J2=" + partida.getPontos(1);

        System.out.println("\n=== FIM DE JOGO === " + msg);
        broadcast(new Mensagem(Mensagem.Tipo.FIM_DE_JOGO, partida));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Leitores paralelos de buzzer
    // ─────────────────────────────────────────────────────────────────────────

    private Thread[] iniciarLeitoresDeBuzzer() {
        Thread[] threads = new Thread[MAX_JOGADORES];
        for (int i = 0; i < MAX_JOGADORES; i++) {
            final int jogador = i;
            threads[i] = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        Object obj = inputs[jogador].readObject();
                        if (!(obj instanceof Mensagem)) continue;
                        Mensagem m = (Mensagem) obj;

                        if (m.getTipo() == Mensagem.Tipo.CLICOU_BUZZER) {
                            if (buzzerVencedor.compareAndSet(-1, jogador)) {
                                buzzerLatch.countDown();
                            }
                        }
                    }
                } catch (EOFException | SocketException ignored) {
                } catch (Exception e) {
                    if (!Thread.currentThread().isInterrupted())
                        System.err.println("  Leitor J" + (jogador + 1) + ": " + e.getMessage());
                }
            }, "BuzzerLeitor-J" + (i + 1));
            threads[i].setDaemon(true);
            threads[i].start();
        }
        return threads;
    }

    /**
     * Aguarda RESPOSTA de um jogador específico com timeout.
     */
    private boolean aguardarResposta(int jogador, int timeoutSeg) throws InterruptedException {
        Thread t = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Object obj = inputs[jogador].readObject();
                    if (!(obj instanceof Mensagem)) continue;
                    Mensagem m = (Mensagem) obj;
                    if (m.getTipo() == Mensagem.Tipo.RESPOSTA) {
                        respostaRecebida.compareAndSet(Integer.MIN_VALUE, (int) m.getDado());
                        respostaLatch.countDown();
                        break;
                    }
                }
            } catch (Exception e) {
                // silencioso — encerra quando interrompido ou timeout
            }
        }, "RespostaLeitor-J" + (jogador + 1));
        t.setDaemon(true);
        t.start();

        boolean recebeu = respostaLatch.await(timeoutSeg, TimeUnit.SECONDS);
        t.interrupt();
        return recebeu;
    }

    private void pararThreads(Thread[] threads) {
        for (Thread t : threads) if (t != null) t.interrupt();
    }


    private synchronized void enviar(int jogador, Mensagem m) {
        try {
            outputs[jogador].writeObject(m);
            outputs[jogador].flush();
            outputs[jogador].reset();
        } catch (IOException e) {
            System.err.println("Erro ao enviar para J" + jogador + ": " + e.getMessage());
        }
    }

    private void broadcast(Mensagem m) {
        for (int i = 0; i < MAX_JOGADORES; i++) enviar(i, m);
    }

    /** Clona a Partida por serialização para evitar envio de referência mutável. */
    private Partida clonarPartida(Partida p) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            new ObjectOutputStream(bos).writeObject(p);
            return (Partida) new ObjectInputStream(
                    new ByteArrayInputStream(bos.toByteArray())).readObject();
        } catch (Exception e) { return p; }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}