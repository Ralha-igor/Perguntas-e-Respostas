package quiz.rede;

import quiz.modelo.Mensagem;
import quiz.modelo.Partida;
import quiz.modelo.Pergunta;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.net.*;

/**
 * Cliente do Quiz Multiplayer - Entrega Parcial 3.
 *
 * Interface gráfica em Java Swing que se conecta ao Servidor via TCP,
 * tratando todos os eventos do protocolo de comunicação:
 *  - IDENTIFICACAO        → define número do jogador
 *  - AGUARDANDO_JOGADOR   → tela de espera / nova partida prestes a começar
 *  - NOVA_PERGUNTA        → exibe pergunta com buzzer bloqueado
 *  - LIBERAR_BUZZER       → habilita botão de buzzer
 *  - VOCE_RESPONDEU       → habilita alternativas + timer 20s
 *  - BUZZER_BLOQUEADO     → informa que adversário está respondendo
 *  - SEGUNDA_CHANCE       → exibe alternativas sem risco de penalidade
 *  - RESULTADO_RODADA     → mostra resultado e placar atualizado
 *  - FIM_DE_JOGO          → tela final com vencedor
 *  - AGUARDANDO_REINICIO  → aguarda adversário confirmar nova partida
 */
public class Cliente extends JFrame {

    // ── Rede ──────────────────────────────────────────────────────────────────
    private static final String HOST  = "127.0.0.1";
    private static final int    PORTA = 12345;

    private ObjectOutputStream output;
    private ObjectInputStream  input;
    private int numeroJogador = -1;

    // ── Cores e fontes ────────────────────────────────────────────────────────
    private static final Color COR_FUNDO        = new Color(18, 18, 30);
    private static final Color COR_PAINEL       = new Color(28, 28, 46);
    private static final Color COR_DESTAQUE     = new Color(99, 102, 241);
    private static final Color COR_VERDE        = new Color(34, 197, 94);
    private static final Color COR_VERMELHO     = new Color(239, 68, 68);
    private static final Color COR_AMARELO      = new Color(234, 179, 8);
    private static final Color COR_TEXTO        = new Color(226, 232, 240);
    private static final Color COR_TEXTO_FRACO  = new Color(148, 163, 184);
    private static final Color COR_BUZZER_OFF   = new Color(55, 55, 80);
    private static final Color COR_BUZZER_ON    = new Color(220, 38, 38);

    private static final Font FONTE_TITULO    = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONTE_PERGUNTA  = new Font("Segoe UI", Font.PLAIN, 16);
    private static final Font FONTE_ALT       = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONTE_PLACAR    = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONTE_STATUS    = new Font("Segoe UI", Font.ITALIC, 13);
    private static final Font FONTE_BUZZER    = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font FONTE_TIMER     = new Font("Segoe UI", Font.BOLD, 20);

    // ── Componentes principais ────────────────────────────────────────────────
    private JPanel painelPrincipal;
    private CardLayout cardLayout;

    // Tela: Conexão
    private JPanel telaConexao;
    private JTextField campoHost;
    private JButton btnConectar;
    private JLabel lblStatusConexao;

    // Tela: Espera
    private JPanel telaEspera;
    private JLabel lblEspera;

    // Tela: Jogo
    private JPanel telaJogo;
    private JLabel lblRodada;
    private JLabel lblPlacar;
    private JLabel lblEnunciado;
    private JButton[] botoesAlternativas = new JButton[4];
    private JButton btnBuzzer;
    private JLabel lblStatus;
    private JLabel lblTimer;

    // Tela: Resultado final
    private JPanel telaFinal;
    private JLabel lblResultadoFinal;
    private JLabel lblPlacarFinal;
    private JButton btnReiniciar;

    // ── Timer de contagem regressiva ──────────────────────────────────────────
    private Timer timerSwing;
    private int segundosRestantes;

    // ── Main ──────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Cliente().setVisible(true));
    }

    // ── Construtor ────────────────────────────────────────────────────────────
    public Cliente() {
        configurarJanela();
        construirTelas();
        mostrarTela("conexao");
    }

    // =========================================================================
    // Configuração da janela
    // =========================================================================

    private void configurarJanela() {
        setTitle("Questions & Answers — Quiz Multiplayer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(720, 540);
        setMinimumSize(new Dimension(640, 480));
        setLocationRelativeTo(null);
        getContentPane().setBackground(COR_FUNDO);
    }

    // =========================================================================
    // Construção das telas
    // =========================================================================

    private void construirTelas() {
        cardLayout      = new CardLayout();
        painelPrincipal = new JPanel(cardLayout);
        painelPrincipal.setBackground(COR_FUNDO);

        telaConexao = construirTelaConexao();
        telaEspera  = construirTelaEspera();
        telaJogo    = construirTelaJogo();
        telaFinal   = construirTelaFinal();

        painelPrincipal.add(telaConexao, "conexao");
        painelPrincipal.add(telaEspera,  "espera");
        painelPrincipal.add(telaJogo,    "jogo");
        painelPrincipal.add(telaFinal,   "final");

        add(painelPrincipal);
    }

    // ── Tela de Conexão ───────────────────────────────────────────────────────
    private JPanel construirTelaConexao() {
        JPanel painel = painelEscuro();
        painel.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 10, 10, 10);
        g.fill = GridBagConstraints.HORIZONTAL;

        JLabel titulo = rotulo("🎯 Questions & Answers", FONTE_TITULO, COR_DESTAQUE);
        titulo.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel subtitulo = rotulo("Quiz Multiplayer em Rede", FONTE_STATUS, COR_TEXTO_FRACO);
        subtitulo.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblHost = rotulo("Endereço do Servidor:", FONTE_ALT, COR_TEXTO);
        campoHost = new JTextField(HOST, 20);
        estilizarCampo(campoHost);

        btnConectar = criarBotao("Conectar", COR_DESTAQUE);
        btnConectar.addActionListener(e -> conectar());

        lblStatusConexao = rotulo("", FONTE_STATUS, COR_TEXTO_FRACO);
        lblStatusConexao.setHorizontalAlignment(SwingConstants.CENTER);

        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        painel.add(titulo, g);
        g.gridy = 1;
        painel.add(subtitulo, g);
        g.gridy = 2; g.gridwidth = 1; g.gridx = 0;
        painel.add(lblHost, g);
        g.gridx = 1;
        painel.add(campoHost, g);
        g.gridy = 3; g.gridx = 0; g.gridwidth = 2;
        painel.add(btnConectar, g);
        g.gridy = 4;
        painel.add(lblStatusConexao, g);

        return painel;
    }

    // ── Tela de Espera ────────────────────────────────────────────────────────
    private JPanel construirTelaEspera() {
        JPanel painel = painelEscuro();
        painel.setLayout(new GridBagLayout());

        lblEspera = rotulo("⏳ Aguardando adversário...", FONTE_TITULO, COR_TEXTO_FRACO);
        lblEspera.setHorizontalAlignment(SwingConstants.CENTER);

        painel.add(lblEspera);
        return painel;
    }

    // ── Tela de Jogo ──────────────────────────────────────────────────────────
    private JPanel construirTelaJogo() {
        JPanel painel = painelEscuro();
        painel.setLayout(new BorderLayout(10, 10));
        painel.setBorder(new EmptyBorder(15, 20, 15, 20));

        // Topo: rodada + placar + timer
        JPanel topo = new JPanel(new BorderLayout());
        topo.setOpaque(false);

        lblRodada = rotulo("Rodada 0/0", FONTE_PLACAR, COR_DESTAQUE);
        lblPlacar = rotulo("J1: 0  |  J2: 0", FONTE_PLACAR, COR_TEXTO);
        lblTimer  = rotulo("", FONTE_TIMER, COR_AMARELO);
        lblTimer.setHorizontalAlignment(SwingConstants.RIGHT);

        topo.add(lblRodada, BorderLayout.WEST);
        topo.add(lblPlacar, BorderLayout.CENTER);
        topo.add(lblTimer,  BorderLayout.EAST);

        // Centro: enunciado
        lblEnunciado = new JLabel("<html><body style='width:500px'>Aguardando pergunta...</body></html>");
        lblEnunciado.setFont(FONTE_PERGUNTA);
        lblEnunciado.setForeground(COR_TEXTO);
        lblEnunciado.setBorder(new EmptyBorder(20, 10, 20, 10));
        lblEnunciado.setHorizontalAlignment(SwingConstants.CENTER);

        // Alternativas
        JPanel painelAlts = new JPanel(new GridLayout(2, 2, 10, 10));
        painelAlts.setOpaque(false);
        String[] letras = {"A", "B", "C", "D"};
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            botoesAlternativas[i] = criarBotao(letras[i] + ") —", COR_PAINEL);
            botoesAlternativas[i].setFont(FONTE_ALT);
            botoesAlternativas[i].setEnabled(false);
            botoesAlternativas[i].addActionListener(e -> enviarResposta(idx));
            painelAlts.add(botoesAlternativas[i]);
        }

        // Buzzer
        btnBuzzer = new JButton("BUZZER");
        btnBuzzer.setFont(FONTE_BUZZER);
        btnBuzzer.setBackground(COR_BUZZER_OFF);
        btnBuzzer.setForeground(Color.WHITE);
        btnBuzzer.setFocusPainted(false);
        btnBuzzer.setBorderPainted(false);
        btnBuzzer.setEnabled(false);
        btnBuzzer.setPreferredSize(new Dimension(160, 60));
        btnBuzzer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnBuzzer.addActionListener(e -> clicouBuzzer());

        // Status
        lblStatus = rotulo("", FONTE_STATUS, COR_TEXTO_FRACO);
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);

        // Rodapé: buzzer + status
        JPanel rodape = new JPanel(new BorderLayout(10, 5));
        rodape.setOpaque(false);
        JPanel centralBuzzer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centralBuzzer.setOpaque(false);
        centralBuzzer.add(btnBuzzer);
        rodape.add(centralBuzzer, BorderLayout.CENTER);
        rodape.add(lblStatus, BorderLayout.SOUTH);

        // Montagem
        JPanel centro = new JPanel(new BorderLayout(0, 10));
        centro.setOpaque(false);
        centro.add(lblEnunciado, BorderLayout.NORTH);
        centro.add(painelAlts,   BorderLayout.CENTER);

        painel.add(topo,   BorderLayout.NORTH);
        painel.add(centro, BorderLayout.CENTER);
        painel.add(rodape, BorderLayout.SOUTH);

        return painel;
    }

    // ── Tela Final ────────────────────────────────────────────────────────────
    private JPanel construirTelaFinal() {
        JPanel painel = painelEscuro();
        painel.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(12, 10, 12, 10);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0;

        lblResultadoFinal = rotulo("", FONTE_TITULO, COR_VERDE);
        lblResultadoFinal.setHorizontalAlignment(SwingConstants.CENTER);

        lblPlacarFinal = rotulo("", FONTE_PLACAR, COR_TEXTO);
        lblPlacarFinal.setHorizontalAlignment(SwingConstants.CENTER);

        btnReiniciar = criarBotao("🔄  Jogar Novamente", COR_DESTAQUE);
        btnReiniciar.addActionListener(e -> reiniciar());

        g.gridy = 0; painel.add(lblResultadoFinal, g);
        g.gridy = 1; painel.add(lblPlacarFinal,    g);
        g.gridy = 2; painel.add(btnReiniciar,       g);

        return painel;
    }

    // =========================================================================
    // Lógica de conexão
    // =========================================================================

    private void conectar() {
        String host = campoHost.getText().trim();
        btnConectar.setEnabled(false);
        lblStatusConexao.setText("Conectando...");
        lblStatusConexao.setForeground(COR_AMARELO);

        new Thread(() -> {
            try {
                Socket socket = new Socket(host, PORTA);
                output = new ObjectOutputStream(socket.getOutputStream());
                output.flush();
                input  = new ObjectInputStream(socket.getInputStream());

                SwingUtilities.invokeLater(() -> lblStatusConexao.setText("Conectado!"));
                ouvirServidor();

            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    lblStatusConexao.setText("Erro: " + ex.getMessage());
                    lblStatusConexao.setForeground(COR_VERMELHO);
                    btnConectar.setEnabled(true);
                });
            }
        }, "Thread-Conexao").start();
    }

    // =========================================================================
    // Loop de escuta do servidor
    // =========================================================================

    private void ouvirServidor() {
        try {
            while (true) {
                Object obj = input.readObject();
                if (!(obj instanceof Mensagem)) continue;
                Mensagem msg = (Mensagem) obj;

                SwingUtilities.invokeLater(() -> tratar(msg));
            }
        } catch (EOFException | SocketException e) {
            SwingUtilities.invokeLater(() ->
                    lblEspera.setText("Conexão encerrada pelo servidor."));
        } catch (Exception e) {
            SwingUtilities.invokeLater(() ->
                    setStatus("Erro na conexão: " + e.getMessage(), COR_VERMELHO));
        }
    }

    // =========================================================================
    // Tratamento de mensagens do servidor
    // =========================================================================

    private void tratar(Mensagem msg) {
        switch (msg.getTipo()) {

            case IDENTIFICACAO:
                numeroJogador = (int) msg.getDado();
                setTitle("Questions & Answers — Jogador " + (numeroJogador + 1));
                lblEspera.setText("👋 Você é o Jogador " + (numeroJogador + 1)
                        + " — aguardando adversário...");
                mostrarTela("espera");
                break;

            case AGUARDANDO_JOGADOR:
                lblEspera.setText("✅ " + msg.getDado());
                mostrarTela("espera");
                break;

            case NOVA_PERGUNTA:
                tratarNovaPergunta((Pergunta) msg.getDado());
                break;

            case LIBERAR_BUZZER:
                liberarBuzzer();
                break;

            case VOCE_RESPONDEU:
                habilitarAlternativas(false);
                iniciarTimer(20);
                setStatus("⚡ Você buzzou primeiro! Escolha a alternativa.", COR_VERDE);
                break;

            case BUZZER_BLOQUEADO:
                bloquearBuzzer();
                setStatus("🔒 Adversário está respondendo...", COR_AMARELO);
                break;

            case SEGUNDA_CHANCE:
                pararTimer();
                habilitarAlternativas(true);
                iniciarTimer(20);
                setStatus("🔄 Segunda chance! Responda sem risco de perder ponto.", COR_AMARELO);
                break;

            case RESULTADO_RODADA:
                tratarResultado((Object[]) msg.getDado());
                break;

            case FIM_DE_JOGO:
                tratarFimDeJogo((Partida) msg.getDado());
                break;

            case AGUARDANDO_REINICIO:
                // Já enviou REINICIAR, agora aguarda o adversário confirmar
                btnReiniciar.setEnabled(false);
                lblPlacarFinal.setText("Aguardando adversário confirmar...");
                break;

            default:
                break;
        }
    }

    // ── Handlers específicos ──────────────────────────────────────────────────

    private void tratarNovaPergunta(Pergunta p) {
        pararTimer();
        mostrarTela("jogo");

        lblEnunciado.setText("<html><body style='width:500px; text-align:center'>"
                + p.getEnunciado() + "</body></html>");

        String[] alts = p.getAlternativas();
        String[] letras = {"A", "B", "C", "D"};
        for (int i = 0; i < 4; i++) {
            botoesAlternativas[i].setText(letras[i] + ")  " + alts[i]);
            botoesAlternativas[i].setBackground(COR_PAINEL);
            botoesAlternativas[i].setEnabled(false);
        }

        bloquearBuzzer();
        setStatus("📖 Leia a pergunta com atenção. O buzzer será liberado em 15s.", COR_TEXTO_FRACO);
    }

    private void tratarResultado(Object[] dados) {
        pararTimer();
        desabilitarAlternativas();
        bloquearBuzzer();

        int     respondente  = (int)     dados[0];
        boolean acertou      = (boolean) dados[1];
        boolean perdeuPonto  = (boolean) dados[2];
        Partida p            = (Partida) dados[3];

        atualizarPlacar(p);

        if (respondente == -1) {
            setStatus("⏰ Ninguém respondeu. Próxima pergunta!", COR_TEXTO_FRACO);
        } else {
            String quem = (respondente == numeroJogador) ? "Você" : "Adversário";
            if (acertou) {
                setStatus("✅ " + quem + " acertou! +1 ponto.", COR_VERDE);
            } else if (perdeuPonto) {
                setStatus("❌ " + quem + " errou ou não respondeu a tempo. -1 ponto.", COR_VERMELHO);
            } else {
                setStatus("❌ " + quem + " errou. Sem penalidade.", COR_VERMELHO);
            }
        }
    }

    private void tratarFimDeJogo(Partida p) {
        pararTimer();
        int venc = p.vencedor();

        if (venc == -1) {
            lblResultadoFinal.setText("🤝 Empate!");
            lblResultadoFinal.setForeground(COR_AMARELO);
        } else if (venc == numeroJogador) {
            lblResultadoFinal.setText("🏆 Você venceu!");
            lblResultadoFinal.setForeground(COR_VERDE);
        } else {
            lblResultadoFinal.setText("😔 Você perdeu!");
            lblResultadoFinal.setForeground(COR_VERMELHO);
        }

        lblPlacarFinal.setText("Jogador 1: " + p.getPontos(0)
                + "  |  Jogador 2: " + p.getPontos(1));

        btnReiniciar.setEnabled(true);
        mostrarTela("final");
    }

    // =========================================================================
    // Ações do jogador
    // =========================================================================

    private void clicouBuzzer() {
        bloquearBuzzer();
        enviarMensagem(new Mensagem(Mensagem.Tipo.CLICOU_BUZZER));
        setStatus("⚡ Buzzer clicado! Aguardando confirmação...", COR_AMARELO);
    }

    private void enviarResposta(int indice) {
        pararTimer();
        desabilitarAlternativas();
        botoesAlternativas[indice].setBackground(COR_DESTAQUE);
        enviarMensagem(new Mensagem(Mensagem.Tipo.RESPOSTA, indice));
        setStatus("✉️ Resposta enviada! Aguardando resultado...", COR_TEXTO_FRACO);
    }

    private void reiniciar() {
        // Desabilita o botão imediatamente para evitar clique duplo
        btnReiniciar.setEnabled(false);
        lblPlacarFinal.setText("Aguardando adversário confirmar...");
        enviarMensagem(new Mensagem(Mensagem.Tipo.REINICIAR));
    }

    // =========================================================================
    // Controles de UI
    // =========================================================================

    private void liberarBuzzer() {
        btnBuzzer.setEnabled(true);
        btnBuzzer.setBackground(COR_BUZZER_ON);
        btnBuzzer.setText("🔴  BUZZER");
        setStatus("🔴 Buzzer liberado! Clique primeiro!", COR_VERMELHO);
    }

    private void bloquearBuzzer() {
        btnBuzzer.setEnabled(false);
        btnBuzzer.setBackground(COR_BUZZER_OFF);
        btnBuzzer.setText("BUZZER");
    }

    private void habilitarAlternativas(boolean segundaChance) {
        bloquearBuzzer();
        for (JButton b : botoesAlternativas) {
            b.setEnabled(true);
            b.setBackground(segundaChance ? new Color(40, 60, 40) : COR_PAINEL);
        }
    }

    private void desabilitarAlternativas() {
        for (JButton b : botoesAlternativas) b.setEnabled(false);
    }

    private void atualizarPlacar(Partida p) {
        lblPlacar.setText("J1: " + p.getPontos(0) + "  |  J2: " + p.getPontos(1));
        lblRodada.setText("Rodada " + p.getRodadaAtual() + "/" + p.getTotalRodadas());
    }

    private void iniciarTimer(int segundos) {
        pararTimer();
        segundosRestantes = segundos;
        lblTimer.setText(segundosRestantes + "s");

        timerSwing = new Timer(1000, e -> {
            segundosRestantes--;
            lblTimer.setText(segundosRestantes + "s");
            lblTimer.setForeground(segundosRestantes <= 5 ? COR_VERMELHO : COR_AMARELO);
            if (segundosRestantes <= 0) pararTimer();
        });
        timerSwing.start();
    }

    private void pararTimer() {
        if (timerSwing != null && timerSwing.isRunning()) timerSwing.stop();
        lblTimer.setText("");
    }

    private void setStatus(String texto, Color cor) {
        lblStatus.setText(texto);
        lblStatus.setForeground(cor);
    }

    private void mostrarTela(String nome) {
        cardLayout.show(painelPrincipal, nome);
    }

    // =========================================================================
    // Comunicação com o servidor
    // =========================================================================

    private synchronized void enviarMensagem(Mensagem m) {
        try {
            output.writeObject(m);
            output.flush();
            output.reset();
        } catch (IOException e) {
            setStatus("Erro ao enviar mensagem: " + e.getMessage(), COR_VERMELHO);
        }
    }

    // =========================================================================
    // Utilitários de estilo
    // =========================================================================

    private JPanel painelEscuro() {
        JPanel p = new JPanel();
        p.setBackground(COR_FUNDO);
        return p;
    }

    private JLabel rotulo(String texto, Font fonte, Color cor) {
        JLabel l = new JLabel(texto);
        l.setFont(fonte);
        l.setForeground(cor);
        return l;
    }

    private JButton criarBotao(String texto, Color fundo) {
        JButton b = new JButton(texto);
        b.setFont(FONTE_ALT);
        b.setBackground(fundo);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(200, 42));
        return b;
    }

    private void estilizarCampo(JTextField campo) {
        campo.setBackground(COR_PAINEL);
        campo.setForeground(COR_TEXTO);
        campo.setCaretColor(COR_TEXTO);
        campo.setFont(FONTE_ALT);
        campo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COR_DESTAQUE, 1),
                new EmptyBorder(5, 8, 5, 8)));
    }
}
