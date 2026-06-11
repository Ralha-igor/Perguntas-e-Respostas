package quiz.rede;

import quiz.modelo.*;
import quiz.util.LeitorPerguntas;

import java.io.*;
import java.net.*;
import java.util.List;

/**
 * Servidor do Quiz Multiplayer - Entrega Parcial 1.
 *
 * Funcionalidades implementadas nesta etapa:
 *  - Carregamento das perguntas a partir de arquivo texto
 *  - Abertura do ServerSocket e escuta na porta definida
 *  - Aceitação de exatamente 2 conexões de clientes
 *  - Identificação de cada jogador (IDENTIFICACAO) via ObjectOutputStream
 *  - Exibição de log no console para cada evento de conexão
 *
 * Próximas etapas (Parciais 2 e 3):
 *  - Lógica completa de rodadas com buzzer e timer
 *  - Envio de perguntas e coleta de respostas
 *  - Controle de placar e fim de jogo
 *  - Interface gráfica no cliente (Java Swing)
 */
public class Servidor {

    private static final int PORTA       = 12345;
    private static final int MAX_JOGADORES = 2;

    // Canais de comunicação com cada cliente
    private ObjectOutputStream[] outputs = new ObjectOutputStream[MAX_JOGADORES];
    private ObjectInputStream[]  inputs  = new ObjectInputStream[MAX_JOGADORES];

    // Perguntas carregadas do arquivo
    private List<Pergunta> perguntas;

    public static void main(String[] args) throws Exception {
        new Servidor().iniciar();
    }

    /**
     * Ponto de entrada do servidor.
     * Carrega as perguntas e aguarda os jogadores conectarem.
     */
    public void iniciar() throws Exception {
        String ip = InetAddress.getLocalHost().getHostAddress();
        System.out.println("=== SERVIDOR QUIZ - PARCIAL 1 ===");
        System.out.println("IP: " + ip + "  Porta: " + PORTA);

        // Carrega perguntas do arquivo texto
        perguntas = LeitorPerguntas.carregar("recursos/perguntas.txt");
        System.out.println("Perguntas carregadas: " + perguntas.size());

        aguardarJogadores();
    }

    
    
    /**
     * Abre o ServerSocket e aceita exatamente 2 clientes.
     * Cada cliente recebe sua identificação (jogador 0 ou 1).
     */
    private void aguardarJogadores() throws Exception {
        System.out.println("\nAguardando " + MAX_JOGADORES + " jogadores na porta " + PORTA + "...");

        try (ServerSocket servidor = new ServerSocket(PORTA)) {

            for (int i = 0; i < MAX_JOGADORES; i++) {
                Socket conexao = servidor.accept();

                // Configura os canais de I/O com o cliente
                outputs[i] = new ObjectOutputStream(conexao.getOutputStream());
                outputs[i].flush();
                inputs[i]  = new ObjectInputStream(conexao.getInputStream());

                // Informa ao cliente qual é o seu número (0 = J1, 1 = J2)
                enviar(i, new Mensagem(Mensagem.Tipo.IDENTIFICACAO, i));

                System.out.println("Jogador " + (i + 1) + " conectado: "
                        + conexao.getInetAddress().getHostAddress());
            }

            
            
            
            // Ambos conectados
            System.out.println("\nAmbos os jogadores conectados!");
            broadcast(new Mensagem(Mensagem.Tipo.AGUARDANDO_JOGADOR,
                    "Ambos conectados! Jogo pronto para iniciar."));

            System.out.println("Perguntas disponíveis para a partida: " + perguntas.size());
            System.out.println("[Parcial 1] Lógica de rodadas será implementada na Parcial 2.");
        }
    }

    
    
    
    private synchronized void enviar(int jogador, Mensagem m) {
        try {
            outputs[jogador].writeObject(m);
            outputs[jogador].flush();
            outputs[jogador].reset();
        } catch (IOException e) {
            System.err.println("Erro ao enviar para jogador " + jogador + ": " + e.getMessage());
        }
    }

    
    
    
    private void broadcast(Mensagem m) {
        for (int i = 0; i < MAX_JOGADORES; i++) {
            enviar(i, m);
        }
    }
}
