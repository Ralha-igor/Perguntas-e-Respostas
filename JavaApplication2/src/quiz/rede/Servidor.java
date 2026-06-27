package quiz.rede;

import quiz.modelo.*;
import quiz.util.LeitorPerguntas;
import quiz.util.HistoricoPartidas;

import java.io.*;
import java.net.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Servidor {

    private static final int PORTA          = 12345;
    private static final int MAX_JOGADORES  = 2;
    private static final int TEMPO_LEITURA  = 15;
    private static final int TEMPO_RESPOSTA = 20;

    // Canais de comunicação
    private final ObjectOutputStream[] outputs = new ObjectOutputStream[MAX_JOGADORES];
    private final ObjectInputStream[]  inputs  = new ObjectInputStream[MAX_JOGADORES];

    // Controles de buzzer (recriados a cada rodada)
    private volatile AtomicInteger  buzzerVencedor;
    private volatile CountDownLatch buzzerLatch;

    // Controles de resposta (recriados a cada tentativa)
    private volatile AtomicInteger  respostaRecebida;
    private volatile CountDownLatch respostaLatch;

    // Controle de reinício: aguarda ambos os jogadores confirmarem
    private volatile CountDownLatch reinicioLatch;

    private List<Pergunta> perguntas;
    private Partida partida;

    // ─────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) throws Exception {
        new Servidor().iniciar();
    }

    public void iniciar() throws Exception {
        String ip = InetAddress.getLocalHost().getHostAddress();
        System.out.println("=== SERVIDOR QUIZ ===");
        System.out.println("IP: " + ip + "  Porta: " + PORTA);

        aguardarJogadores();
        iniciarThreadsDeEscuta();

        // Loop principal: joga e reinicia enquanto os clientes quiserem
        do {
            perguntas = LeitorPerguntas.carregar("recursos/perguntas.txt");
            System.out.println("Perguntas carregadas: " + perguntas.size());
            perguntas = perguntas.subList(0, 3);
            iniciarPartida();
        } while (aguardarReinicio());
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void aguardarJogadores() throws Exception {
        System.out.println("\nAguardando " + MAX_JOGADORES + " jogadores na porta " + PORTA + "...");

        ServerSocket servidor = new ServerSocket(PORTA);

        for (int i = 0; i < MAX_JOGADORES; i++) {
            Socket conexao = servidor.accept();

            outputs[i] = new ObjectOutputStream(conexao.getOutputStream());
            outputs[i].flush();
            inputs[i]  = new ObjectInputStream(conexao.getInputStream());

            enviar(i, new Mensagem(Mensagem.Tipo.IDENTIFICACAO, i));
            System.out.println("Jogador " + (i + 1) + " conectado: "
                    + conexao.getInetAddress().getHostAddress());
        }

        servidor.close();

        System.out.println("\nAmbos conectados! Iniciando partida...");
        broadcast(new Mensagem(Mensagem.Tipo.AGUARDANDO_JOGADOR,
                "Ambos conectados! Jogo iniciando..."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    /**
     * Inicia threads permanentes de escuta para cada jogador.
     * Além do buzzer e resposta, agora também captura REINICIAR.
     */
    private void iniciarThreadsDeEscuta() {
        for (int i = 0; i < MAX_JOGADORES; i++) {
            final int jogador = i;
            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        Object obj = inputs[jogador].readObject();
                        if (!(obj instanceof Mensagem)) continue;
                        Mensagem m = (Mensagem) obj;

                        switch (m.getTipo()) {
                            case CLICOU_BUZZER:
                                if (buzzerVencedor != null
                                        && buzzerVencedor.compareAndSet(-1, jogador)) {
                                    buzzerLatch.countDown();
                                }
                                break;

                            case RESPOSTA:
                                if (respostaRecebida != null) {
                                    respostaRecebida.compareAndSet(
                                            Integer.MIN_VALUE, (int) m.getDado());
                                    respostaLatch.countDown();
                                }
                                break;

                            case REINICIAR:
                                System.out.println("  Jogador " + (jogador + 1)
                                        + " quer reiniciar.");
                                if (reinicioLatch != null) {
                                    // Avisa quem clicou que está esperando o adversário
                                    enviar(jogador, new Mensagem(Mensagem.Tipo.AGUARDANDO_REINICIO, null));
                                    reinicioLatch.countDown();
                                }
                                break;

                            default:
                                break;
                        }
                    }
                } catch (EOFException | SocketException e) {
                    System.out.println("Jogador " + (jogador + 1) + " desconectou.");
                } catch (Exception e) {
                    System.err.println("Escuta J" + (jogador + 1) + ": " + e.getMessage());
                }
            }, "Escuta-J" + (i + 1));
            t.setDaemon(true);
            t.start();
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
    /**
     * Aguarda ambos os jogadores enviarem REINICIAR.
     * Envia AGUARDANDO_REINICIO para que o cliente que já clicou
     * saiba que está esperando o adversário.
     *
     * @return true se ambos confirmaram; false se conexão caiu
     */
    private boolean aguardarReinicio() throws InterruptedException {
        reinicioLatch = new CountDownLatch(MAX_JOGADORES);
        System.out.println("\nAguardando jogadores para reiniciar...");

        boolean ambosConfirmaram = reinicioLatch.await(5, TimeUnit.MINUTES);

        if (ambosConfirmaram) {
            System.out.println("Reiniciando partida!");
            broadcast(new Mensagem(Mensagem.Tipo.AGUARDANDO_JOGADOR,
                    "Ambos prontos! Nova partida iniciando..."));
            sleep(1500);
        }

        return ambosConfirmaram;
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void executarRodada(int indice) throws Exception {
        Pergunta pergunta = perguntas.get(indice);

        System.out.printf("%n── Rodada %d/%d: %s%n",
                indice + 1, partida.getTotalRodadas(), pergunta.getEnunciado());

        // Reseta controles para esta rodada
        buzzerVencedor   = new AtomicInteger(-1);
        buzzerLatch      = new CountDownLatch(1);
        respostaRecebida = new AtomicInteger(Integer.MIN_VALUE);
        respostaLatch    = new CountDownLatch(1);

        broadcast(new Mensagem(Mensagem.Tipo.NOVA_PERGUNTA, pergunta));

        System.out.println("  Leitura: " + TEMPO_LEITURA + "s...");
        sleep(TEMPO_LEITURA * 1000L);

        broadcast(new Mensagem(Mensagem.Tipo.LIBERAR_BUZZER, null));
        System.out.println("  Buzzer liberado! Aguardando clique...");

        boolean alguemClicou = buzzerLatch.await(TEMPO_RESPOSTA, TimeUnit.SECONDS);

        if (!alguemClicou) {
            System.out.println("  Ninguém clicou — rodada encerrada.");
            broadcast(new Mensagem(Mensagem.Tipo.RESULTADO_RODADA,
                    new Object[]{-1, false, false, clonarPartida(partida)}));
            return;
        }

        int clicante  = buzzerVencedor.get();
        int adversario = 1 - clicante;
        System.out.println("  Jogador " + (clicante + 1) + " clicou primeiro!");

        enviar(clicante,   new Mensagem(Mensagem.Tipo.VOCE_RESPONDEU, null));
        enviar(adversario, new Mensagem(Mensagem.Tipo.BUZZER_BLOQUEADO, null));

        respostaRecebida = new AtomicInteger(Integer.MIN_VALUE);
        respostaLatch    = new CountDownLatch(1);

        boolean respondeu = respostaLatch.await(TEMPO_RESPOSTA, TimeUnit.SECONDS);
        int resposta = respostaRecebida.get();

        if (!respondeu || resposta == Integer.MIN_VALUE) {
            System.out.println("  J" + (clicante + 1) + " não respondeu → -1 ponto.");
            partida.removerPonto(clicante);
            broadcast(new Mensagem(Mensagem.Tipo.RESULTADO_RODADA,
                    new Object[]{clicante, false, true, clonarPartida(partida)}));

            executarSegundaChance(adversario, pergunta);

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

            executarSegundaChance(adversario, pergunta);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void executarSegundaChance(int jogador, Pergunta pergunta) throws Exception {
        System.out.println("  Segunda chance: J" + (jogador + 1) + " (sem risco)...");

        respostaRecebida = new AtomicInteger(Integer.MIN_VALUE);
        respostaLatch    = new CountDownLatch(1);

        enviar(jogador, new Mensagem(Mensagem.Tipo.SEGUNDA_CHANCE, pergunta));

        boolean respondeu = respostaLatch.await(TEMPO_RESPOSTA, TimeUnit.SECONDS);
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
            System.out.println("  J" + (jogador + 1) + " errou na segunda chance → sem penalidade.");
            broadcast(new Mensagem(Mensagem.Tipo.RESULTADO_RODADA,
                    new Object[]{jogador, false, false, clonarPartida(partida)}));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void encerrarJogo() {
        int venc = partida.vencedor();
        String msg = venc == -1
                ? "Empate! J1=" + partida.getPontos(0) + " J2=" + partida.getPontos(1)
                : "Vencedor: J" + (venc + 1) + "! Placar: J1="
                        + partida.getPontos(0) + " J2=" + partida.getPontos(1);

        System.out.println("\n=== FIM DE JOGO === " + msg);
        broadcast(new Mensagem(Mensagem.Tipo.FIM_DE_JOGO, partida));
        HistoricoPartidas.registrar(partida);
        HistoricoPartidas.exibir();
    }

    // ─────────────────────────────────────────────────────────────────────────
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
