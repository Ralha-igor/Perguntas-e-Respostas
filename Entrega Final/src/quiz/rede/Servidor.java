package quiz.rede;

import quiz.modelo.*;
import quiz.util.LeitorPerguntas;
import quiz.util.HistoricoPartidas;
import quiz.modelo.EventoAcerto;
import quiz.modelo.EventoErro;
import quiz.modelo.EventoTimeout;
import quiz.modelo.EventoSemResposta;
import java.util.function.BiConsumer;

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
    private static final int DELAY_RODADA   = 3; // segundos entre resultado e próxima pergunta

    private final ObjectOutputStream[] outputs = new ObjectOutputStream[MAX_JOGADORES];
    private final ObjectInputStream[]  inputs  = new ObjectInputStream[MAX_JOGADORES];

    private volatile AtomicInteger  buzzerVencedor;
    private volatile CountDownLatch buzzerLatch;
    private volatile AtomicInteger  respostaRecebida;
    private volatile CountDownLatch respostaLatch;
    private volatile CountDownLatch reinicioLatch;

    // Nomes recebidos dos clientes
    private final String[] nomes = new String[]{"Player 1", "Player 2"};
    private volatile CountDownLatch nomesLatch;

    private List<Pergunta> perguntas;
    private Partida partida;

    public static void main(String[] args) throws Exception {
        new Servidor().iniciar();
    }

    public void iniciar() throws Exception {
        String ip = InetAddress.getLocalHost().getHostAddress();
        System.out.println("=== SERVIDOR QUIZ ===");
        System.out.println("IP: " + ip + "  Porta: " + PORTA);

        aguardarJogadores();
        iniciarThreadsDeEscuta();

        do {
            perguntas = LeitorPerguntas.carregar("recursos/perguntas.txt");
            System.out.println("Perguntas carregadas: " + perguntas.size());
//            perguntas = perguntas.subList(0, 3);
            iniciarPartida();
        } while (aguardarReinicio());
    }

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

    /**
     * Threads permanentes de escuta: tratam CLICOU_BUZZER, RESPOSTA,
     * REINICIAR e NOME (nome customizado do jogador).
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
                                System.out.println("  Jogador " + (jogador + 1) + " quer reiniciar.");
                                if (reinicioLatch != null) {
                                    enviar(jogador, new Mensagem(Mensagem.Tipo.AGUARDANDO_REINICIO, null));
                                    reinicioLatch.countDown();
                                }
                                break;

                            case NOME:
                                String nome = (String) m.getDado();
                                nomes[jogador] = (nome != null && !nome.trim().isEmpty())
                                        ? nome.trim() : "Player " + (jogador + 1);
                                System.out.println("  J" + (jogador + 1) + " se chama: " + nomes[jogador]);
                                if (nomesLatch != null) nomesLatch.countDown();
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

    /**
     * Aguarda os dois jogadores enviarem seus nomes antes de iniciar a partida.
     * Após receber ambos, envia NOMES_JOGADORES para que os cards sejam
     * atualizados em ambas as telas imediatamente.
     */
    private void aguardarNomes() throws InterruptedException {
        System.out.println("Aguardando nomes dos jogadores...");
        nomesLatch.await(10, TimeUnit.SECONDS); // timeout de 10s
        System.out.println("Nomes recebidos: " + nomes[0] + ", " + nomes[1]);
        broadcast(new Mensagem(Mensagem.Tipo.NOMES_JOGADORES, new String[]{nomes[0], nomes[1]}));
    }

    private void iniciarPartida() throws Exception {
        partida = new Partida(perguntas.size());
        // Aplica nomes recebidos dos clientes
        partida.setNome(0, nomes[0]);
        partida.setNome(1, nomes[1]);

        while (!partida.finalizada()) {
            executarRodada(partida.getRodadaAtual());
            partida.proximaRodada();
        }

        encerrarJogo();
    }

    private boolean aguardarReinicio() throws InterruptedException {
        reinicioLatch = new CountDownLatch(MAX_JOGADORES);
        System.out.println("\nAguardando jogadores para reiniciar...");

        boolean ambosConfirmaram = reinicioLatch.await(5, TimeUnit.MINUTES);

        if (ambosConfirmaram) {
            System.out.println("Reiniciando partida!");
            broadcast(new Mensagem(Mensagem.Tipo.AGUARDANDO_JOGADOR,
                    "Ambos prontos! Nova partida iniciando..."));
            broadcast(new Mensagem(Mensagem.Tipo.NOMES_JOGADORES, new String[]{nomes[0], nomes[1]}));
            sleep(1500);
        }

        return ambosConfirmaram;
    }

    private void executarRodada(int indice) throws Exception {
        Pergunta pergunta = perguntas.get(indice);
        int indiceCorreto = pergunta.getIndiceCorreto();

        System.out.printf("%n── Rodada %d/%d: %s%n",
                indice + 1, partida.getTotalRodadas(), pergunta.getEnunciado());

        buzzerVencedor   = new AtomicInteger(-1);
        buzzerLatch      = new CountDownLatch(1);
        respostaRecebida = new AtomicInteger(Integer.MIN_VALUE);
        respostaLatch    = new CountDownLatch(1);

        broadcast(new Mensagem(Mensagem.Tipo.NOVA_PERGUNTA,
                new PerguntaRodada(pergunta, indice + 1, partida.getTotalRodadas())));

        System.out.println("  Leitura: " + TEMPO_LEITURA + "s...");
        sleep(TEMPO_LEITURA * 1000L);

        broadcast(new Mensagem(Mensagem.Tipo.LIBERAR_BUZZER, null));
        System.out.println("  Buzzer liberado!");

        boolean alguemClicou = buzzerLatch.await(TEMPO_RESPOSTA, TimeUnit.SECONDS);

        if (!alguemClicou) {
            System.out.println("  Ninguém clicou.");
            new EventoSemResposta(partida, indiceCorreto, fnPontos(), this::broadcast).aplicar();
            sleep(DELAY_RODADA * 1000L);
            return;
        }

        int clicante  = buzzerVencedor.get();
        int adversario = 1 - clicante;
        System.out.println("  J" + (clicante + 1) + " clicou primeiro!");

        enviar(clicante,   new Mensagem(Mensagem.Tipo.VOCE_RESPONDEU, null));
        enviar(adversario, new Mensagem(Mensagem.Tipo.BUZZER_BLOQUEADO, TEMPO_RESPOSTA));

        respostaRecebida = new AtomicInteger(Integer.MIN_VALUE);
        respostaLatch    = new CountDownLatch(1);

        boolean respondeu = respostaLatch.await(TEMPO_RESPOSTA, TimeUnit.SECONDS);
        int resposta = respostaRecebida.get();

        if (!respondeu || resposta == Integer.MIN_VALUE) {
            System.out.println("  J" + (clicante + 1) + " não respondeu → perde a vez.");
            new EventoTimeout(clicante, partida, indiceCorreto, fnPontos(), this::broadcast).aplicar();
            sleep(DELAY_RODADA * 1000L);
            executarSegundaChance(adversario, pergunta);

        } else if (pergunta.verificarResposta(resposta)) {
            System.out.println("  J" + (clicante + 1) + " ACERTOU!");
            new EventoAcerto(clicante, partida, indiceCorreto, fnPontos(), this::broadcast).aplicar();
            sleep(DELAY_RODADA * 1000L);

        } else {
            System.out.println("  J" + (clicante + 1) + " ERROU → perde a vez.");
            new EventoErro(clicante, partida, indiceCorreto, fnPontos(), this::broadcast).aplicar();
            sleep(DELAY_RODADA * 1000L);
            executarSegundaChance(adversario, pergunta);
        }
    }

    private void executarSegundaChance(int jogador, Pergunta pergunta) throws Exception {
        System.out.println("  Segunda chance: J" + (jogador + 1) + "...");
        int indiceCorreto = pergunta.getIndiceCorreto();

        respostaRecebida = new AtomicInteger(Integer.MIN_VALUE);
        respostaLatch    = new CountDownLatch(1);

        enviar(jogador, new Mensagem(Mensagem.Tipo.SEGUNDA_CHANCE, pergunta));

        boolean respondeu = respostaLatch.await(TEMPO_RESPOSTA, TimeUnit.SECONDS);
        int resposta = respostaRecebida.get();

        if (!respondeu || resposta == Integer.MIN_VALUE) {
            System.out.println("  J" + (jogador + 1) + " não respondeu na segunda chance.");
            new EventoSemResposta(partida, indiceCorreto, fnPontos(), this::broadcast).aplicar();
        } else if (pergunta.verificarResposta(resposta)) {
            System.out.println("  J" + (jogador + 1) + " acertou na segunda chance!");
            new EventoAcerto(jogador, partida, indiceCorreto, fnPontos(), this::broadcast).aplicar();
        } else {
            System.out.println("  J" + (jogador + 1) + " errou na segunda chance.");
            new EventoSemResposta(partida, indiceCorreto, fnPontos(), this::broadcast).aplicar();
        }

        sleep(DELAY_RODADA * 1000L);
    }

    /**
     * Retorna a função de alteração de pontos usada pelos EventoJogo.
     * Polimorfismo: Servidor passa esse callback para qualquer subclasse
     * de EventoJogo sem saber qual tipo concreto é.
     */
    private BiConsumer<Integer, Integer> fnPontos() {
        return (jogador, delta) -> {
            if (delta > 0) partida.adicionarPonto(jogador);
        };
    }

    private void encerrarJogo() {
        int venc = partida.vencedor();
        String msg = venc == -1
                ? "Empate! " + nomes[0] + "=" + partida.getPontos(0)
                        + " " + nomes[1] + "=" + partida.getPontos(1)
                : "Vencedor: " + nomes[venc] + "! Placar: "
                        + nomes[0] + "=" + partida.getPontos(0)
                        + " " + nomes[1] + "=" + partida.getPontos(1);

        System.out.println("\n=== FIM DE JOGO === " + msg);
        broadcast(new Mensagem(Mensagem.Tipo.FIM_DE_JOGO, partida));
        HistoricoPartidas.registrar(partida);
        HistoricoPartidas.exibir();
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

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
