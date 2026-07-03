package quiz.rede;

import quiz.modelo.Mensagem;
import quiz.modelo.Partida;
import quiz.modelo.Pergunta;
import quiz.modelo.PerguntaRodada;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.io.*;
import java.net.*;


public class Cliente extends JFrame {

    // ── Rede ──────────────────────────────────────────────────────────────────
    private static final String HOST_PADRAO = "127.0.0.1";
    private static final int    PORTA       = 12345;

    private ObjectOutputStream output;
    private ObjectInputStream  input;
    private int    numeroJogador = -1;
    private String nomeJogador   = "";

    // ── Cores ─────────────────────────────────────────────────────────────────
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
    private static final Color COR_CARD_EU      = new Color(30, 40, 80);
    private static final Color COR_CARD_ADV     = new Color(40, 25, 55);
    private static final Color COR_ALT_CORRETA  = new Color(22, 101, 52);  // verde escuro
    private static final Color COR_ALT_ERRADA   = new Color(127, 29, 29);  // vermelho escuro

    // ── Fontes ────────────────────────────────────────────────────────────────
    private static final Font FONTE_TITULO      = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONTE_PERGUNTA    = new Font("Segoe UI", Font.PLAIN, 16);
    private static final Font FONTE_ALT         = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONTE_STATUS      = new Font("Segoe UI", Font.ITALIC, 13);
    private static final Font FONTE_BUZZER      = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font FONTE_TIMER       = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font FONTE_CARD_NOME   = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONTE_CARD_PONTOS = new Font("Segoe UI", Font.BOLD, 32);
    private static final Font FONTE_RODADA      = new Font("Segoe UI", Font.BOLD, 13);

    // ── Componentes ───────────────────────────────────────────────────────────
    private JPanel    painelPrincipal;
    private CardLayout cardLayout;

    // Tela: Conexão
    private JTextField campoHost;
    private JTextField campoNome;
    private JButton    btnConectar;
    private JLabel     lblStatusConexao;

    // Tela: Espera
    private JLabel lblEspera;
    private JLabel lblEsperaSubtitulo;

    // Tela: Jogo
    private JLabel    lblRodada;
    private JLabel    lblEnunciado;
    private JButton[] botoesAlternativas = new JButton[4];
    private JButton   btnBuzzer;
    private JLabel    lblStatus;
    private JLabel    lblTimer;
    private JLabel    lblNomeEu;
    private JLabel    lblNomeAdv;
    private JLabel    lblPontosEu;
    private JLabel    lblPontosAdv;

    // Tela: Final
    private JLabel  lblResultadoFinal;
    private JLabel  lblPlacarFinal;
    private JButton btnReiniciar;

    // ── Timer ─────────────────────────────────────────────────────────────────
    private Timer timerSwing;
    private int   segundosRestantes;

    // ── Última resposta enviada (para destacar após resultado) ─────────────────
    private int ultimaRespostaEnviada = -1;

    // ── Main ──────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Cliente().setVisible(true));
    }

    public Cliente() {
        configurarJanela();
        construirTelas();
        mostrarTela("conexao");
    }

    // =========================================================================
    // Janela
    // =========================================================================

    private void configurarJanela() {
        setTitle("Questions & Answers — Quiz Multiplayer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(760, 580);
        setMinimumSize(new Dimension(660, 520));
        setLocationRelativeTo(null);
        getContentPane().setBackground(COR_FUNDO);
    }

    // =========================================================================
    // Telas
    // =========================================================================

    private void construirTelas() {
        cardLayout      = new CardLayout();
        painelPrincipal = new JPanel(cardLayout);
        painelPrincipal.setBackground(COR_FUNDO);

        painelPrincipal.add(construirTelaConexao(), "conexao");
        painelPrincipal.add(construirTelaEspera(),  "espera");
        painelPrincipal.add(construirTelaJogo(),    "jogo");
        painelPrincipal.add(construirTelaFinal(),   "final");

        add(painelPrincipal);
    }

    // ── Conexão ───────────────────────────────────────────────────────────────
    private JPanel construirTelaConexao() {
        JPanel painel = painelEscuro();
        painel.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 10, 10, 10);
        g.fill   = GridBagConstraints.HORIZONTAL;

        JLabel titulo    = rotulo("🎯 Questions & Answers", FONTE_TITULO, COR_DESTAQUE);
        JLabel subtitulo = rotulo("Quiz Multiplayer em Rede", FONTE_STATUS, COR_TEXTO_FRACO);
        titulo.setHorizontalAlignment(SwingConstants.CENTER);
        subtitulo.setHorizontalAlignment(SwingConstants.CENTER);

        JLabel lblHost = rotulo("Endereço do Servidor:", FONTE_ALT, COR_TEXTO);
        campoHost = new JTextField(HOST_PADRAO, 20);
        estilizarCampo(campoHost);

        JLabel lblNomeLbl = rotulo("Seu nome:", FONTE_ALT, COR_TEXTO);
        campoNome = new JTextField("", 20);
        campoNome.setToolTipText("Digite seu nome (opcional)");
        estilizarCampo(campoNome);

        btnConectar = criarBotao("Conectar", COR_DESTAQUE);
        btnConectar.addActionListener(e -> conectar());

        // Enter no campo nome também conecta
        campoNome.addActionListener(e -> conectar());
        campoHost.addActionListener(e -> campoNome.requestFocus());

        lblStatusConexao = rotulo("", FONTE_STATUS, COR_TEXTO_FRACO);
        lblStatusConexao.setHorizontalAlignment(SwingConstants.CENTER);

        g.gridx = 0; g.gridy = 0; g.gridwidth = 2; painel.add(titulo, g);
        g.gridy = 1; painel.add(subtitulo, g);
        g.gridy = 2; g.gridwidth = 1; g.gridx = 0; painel.add(lblHost, g);
        g.gridx = 1; painel.add(campoHost, g);
        g.gridy = 3; g.gridx = 0; painel.add(lblNomeLbl, g);
        g.gridx = 1; painel.add(campoNome, g);
        g.gridy = 4; g.gridx = 0; g.gridwidth = 2; painel.add(btnConectar, g);
        g.gridy = 5; painel.add(lblStatusConexao, g);

        return painel;
    }

    // ── Espera ────────────────────────────────────────────────────────────────
    private JPanel construirTelaEspera() {
        JPanel painel = painelEscuro();
        painel.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.insets = new Insets(8, 10, 8, 10);

        lblEspera = rotulo("⏳ Aguardando adversário...", FONTE_TITULO, COR_TEXTO_FRACO);
        lblEspera.setHorizontalAlignment(SwingConstants.CENTER);

        lblEsperaSubtitulo = rotulo("", FONTE_STATUS, COR_TEXTO_FRACO);
        lblEsperaSubtitulo.setHorizontalAlignment(SwingConstants.CENTER);

        g.gridy = 0; painel.add(lblEspera, g);
        g.gridy = 1; painel.add(lblEsperaSubtitulo, g);
        return painel;
    }

    // ── Jogo ──────────────────────────────────────────────────────────────────
    private JPanel construirTelaJogo() {
        JPanel painel = painelEscuro();
        painel.setLayout(new BorderLayout(10, 10));
        painel.setBorder(new EmptyBorder(12, 16, 12, 16));

        // Topo
        JPanel topo = new JPanel(new BorderLayout(10, 0));
        topo.setOpaque(false);

        lblRodada = rotulo("Rodada 0/0", FONTE_RODADA, COR_DESTAQUE);
        lblRodada.setHorizontalAlignment(SwingConstants.CENTER);

        lblTimer = rotulo("", FONTE_TIMER, COR_AMARELO);
        lblTimer.setHorizontalAlignment(SwingConstants.RIGHT);

        JPanel painelPlacares = new JPanel(new GridLayout(1, 2, 10, 0));
        painelPlacares.setOpaque(false);

        JPanel cardEu  = criarCardPlacar("Você",        COR_CARD_EU,  new Color(99, 102, 241));
        JPanel cardAdv = criarCardPlacar("Adversário",  COR_CARD_ADV, new Color(168, 85, 247));

        lblNomeEu   = (JLabel) cardEu.getClientProperty("nome");
        lblNomeAdv  = (JLabel) cardAdv.getClientProperty("nome");
        lblPontosEu  = (JLabel) cardEu.getClientProperty("pontos");
        lblPontosAdv = (JLabel) cardAdv.getClientProperty("pontos");

        painelPlacares.add(cardEu);
        painelPlacares.add(cardAdv);

        topo.add(lblRodada,      BorderLayout.NORTH);
        topo.add(painelPlacares, BorderLayout.CENTER);
        topo.add(lblTimer,       BorderLayout.EAST);

        // Centro
        lblEnunciado = new JLabel("<html><body style='width:460px; text-align:center'>Aguardando pergunta...</body></html>");
        lblEnunciado.setFont(FONTE_PERGUNTA);
        lblEnunciado.setForeground(COR_TEXTO);
        lblEnunciado.setBorder(new EmptyBorder(16, 10, 16, 10));
        lblEnunciado.setHorizontalAlignment(SwingConstants.CENTER);

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

        JPanel centro = new JPanel(new BorderLayout(0, 10));
        centro.setOpaque(false);
        centro.add(lblEnunciado, BorderLayout.NORTH);
        centro.add(painelAlts,   BorderLayout.CENTER);

        // Rodapé
        btnBuzzer = new JButton("BUZZER");
        btnBuzzer.setFont(FONTE_BUZZER);
        btnBuzzer.setBackground(COR_BUZZER_OFF);
        btnBuzzer.setForeground(Color.WHITE);
        btnBuzzer.setFocusPainted(false);
        btnBuzzer.setBorderPainted(false);
        btnBuzzer.setEnabled(false);
        btnBuzzer.setPreferredSize(new Dimension(160, 56));
        btnBuzzer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnBuzzer.addActionListener(e -> clicouBuzzer());

        lblStatus = rotulo("", FONTE_STATUS, COR_TEXTO_FRACO);
        lblStatus.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel centralBuzzer = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centralBuzzer.setOpaque(false);
        centralBuzzer.add(btnBuzzer);

        JPanel rodape = new JPanel(new BorderLayout(0, 4));
        rodape.setOpaque(false);
        rodape.add(centralBuzzer, BorderLayout.CENTER);
        rodape.add(lblStatus,     BorderLayout.SOUTH);

        painel.add(topo,   BorderLayout.NORTH);
        painel.add(centro, BorderLayout.CENTER);
        painel.add(rodape, BorderLayout.SOUTH);

        return painel;
    }

    private JPanel criarCardPlacar(String nomeInicial, Color fundoCard, Color corBorda) {
        JPanel card = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBackground(fundoCard);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(corBorda, 2, true),
                new EmptyBorder(8, 12, 8, 12)));

        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.anchor = GridBagConstraints.CENTER;

        JLabel lblNome   = rotulo(nomeInicial, FONTE_CARD_NOME, COR_TEXTO_FRACO);
        JLabel lblPontos = rotulo("0", FONTE_CARD_PONTOS, COR_TEXTO);
        JLabel lblPt     = rotulo("pts", FONTE_STATUS, COR_TEXTO_FRACO);
        lblNome.setHorizontalAlignment(SwingConstants.CENTER);
        lblPontos.setHorizontalAlignment(SwingConstants.CENTER);
        lblPt.setHorizontalAlignment(SwingConstants.CENTER);

        g.gridy = 0; card.add(lblNome,   g);
        g.gridy = 1; card.add(lblPontos, g);
        g.gridy = 2; card.add(lblPt,     g);

        card.putClientProperty("nome",   lblNome);
        card.putClientProperty("pontos", lblPontos);

        return card;
    }

    // ── Final ─────────────────────────────────────────────────────────────────
    private JPanel construirTelaFinal() {
        JPanel painel = painelEscuro();
        painel.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(12, 10, 12, 10);
        g.fill   = GridBagConstraints.HORIZONTAL;
        g.gridx  = 0;

        lblResultadoFinal = rotulo("", FONTE_TITULO, COR_VERDE);
        lblResultadoFinal.setHorizontalAlignment(SwingConstants.CENTER);

        lblPlacarFinal = rotulo("", FONTE_STATUS, COR_TEXTO);
        lblPlacarFinal.setHorizontalAlignment(SwingConstants.CENTER);

        btnReiniciar = criarBotao("🔄  Jogar Novamente", COR_DESTAQUE);
        btnReiniciar.addActionListener(e -> reiniciar());

        g.gridy = 0; painel.add(lblResultadoFinal, g);
        g.gridy = 1; painel.add(lblPlacarFinal,    g);
        g.gridy = 2; painel.add(btnReiniciar,       g);

        return painel;
    }

    // =========================================================================
    // Conexão
    // =========================================================================

    private void conectar() {
        String host = campoHost.getText().trim();
        nomeJogador = campoNome.getText().trim();
        if (nomeJogador.isEmpty()) nomeJogador = "Player";

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
    // Escuta do servidor
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
            SwingUtilities.invokeLater(() -> lblEspera.setText("Conexão encerrada."));
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> setStatus("Erro: " + e.getMessage(), COR_VERMELHO));
        }
    }

    // =========================================================================
    // Tratamento de mensagens
    // =========================================================================

    private void tratar(Mensagem msg) {
        switch (msg.getTipo()) {

            case IDENTIFICACAO:
                numeroJogador = (int) msg.getDado();
                setTitle("Questions & Answers — " + nomeJogador);
                // Envia o nome ao servidor
                enviarMensagem(new Mensagem(Mensagem.Tipo.NOME, nomeJogador));
                lblNomeEu.setText(nomeJogador);
                lblEspera.setText("👋 Olá, " + nomeJogador + "! Aguardando adversário...");
                mostrarTela("espera");
                break;

            case AGUARDANDO_JOGADOR:
                lblEspera.setText("✅ " + msg.getDado());
                // Zera placar dos cards para nova partida
                lblPontosEu.setText("0");
                lblPontosAdv.setText("0");
                mostrarTela("espera");
                break;

            case NOVA_PERGUNTA:
                tratarNovaPergunta((PerguntaRodada) msg.getDado());
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
                // Recebe o tempo restante para mostrar timer ao adversário
                bloquearBuzzer();
                int tempo = msg.getDado() != null ? (int) msg.getDado() : 20;
                iniciarTimer(tempo);
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

            case NOMES_JOGADORES:
                // Servidor enviou nomes dos dois jogadores — atualiza cards imediatamente
                String[] nomesRecebidos = (String[]) msg.getDado();
                lblNomeEu.setText(nomesRecebidos[numeroJogador]);
                lblNomeAdv.setText(nomesRecebidos[1 - numeroJogador]);
                break;

            case AGUARDANDO_REINICIO:
                btnReiniciar.setEnabled(false);
                lblPlacarFinal.setText("Aguardando adversário confirmar...");
                break;

            default:
                break;
        }
    }

    // ── Handlers ──────────────────────────────────────────────────────────────

    private void tratarNovaPergunta(PerguntaRodada pr) {
        pararTimer();
        ultimaRespostaEnviada = -1;
        mostrarTela("jogo");

        Pergunta p = pr.getPergunta();
        lblRodada.setText("Rodada " + pr.getRodadaAtual() + " / " + pr.getTotalRodadas());
        lblEnunciado.setText("<html><body style='width:460px; text-align:center'>"
                + p.getEnunciado() + "</body></html>");

        String[] alts  = p.getAlternativas();
        String[] letras = {"A", "B", "C", "D"};
        for (int i = 0; i < 4; i++) {
            botoesAlternativas[i].setText(letras[i] + ")  " + alts[i]);
            botoesAlternativas[i].setBackground(COR_PAINEL);
            botoesAlternativas[i].setEnabled(false);
        }

        bloquearBuzzer();
        setStatus("📖 Leia a pergunta. O buzzer será liberado em 15s.", COR_TEXTO_FRACO);
    }

    private void tratarResultado(Object[] dados) {
        pararTimer();
        desabilitarAlternativas();
        bloquearBuzzer();

        int     respondente   = (int)     dados[0];
        boolean acertou       = (boolean) dados[1];
        int     indiceCorreto = (int)     dados[2];
        Partida p             = (Partida) dados[3];

        // Destaca a resposta correta em verde
        if (indiceCorreto >= 0 && indiceCorreto < 4) {
            botoesAlternativas[indiceCorreto].setBackground(COR_ALT_CORRETA);
        }

        // Destaca a resposta errada do jogador em vermelho (se foi ele quem errou)
        if (ultimaRespostaEnviada >= 0
                && ultimaRespostaEnviada != indiceCorreto
                && respondente == numeroJogador) {
            botoesAlternativas[ultimaRespostaEnviada].setBackground(COR_ALT_ERRADA);
        }

        // Atualiza nomes dos cards com os nomes reais recebidos pelo servidor
        lblNomeAdv.setText(p.getNome(1 - numeroJogador));

        atualizarPlacar(p);

        if (respondente == -1) {
            setStatus("⏰ Ninguém respondeu. Próxima pergunta em breve...", COR_TEXTO_FRACO);
        } else {
            String quem = (respondente == numeroJogador) ? "Você" : "Adversário";
            if (acertou) {
                setStatus("✅ " + quem + " acertou! +1 ponto.", COR_VERDE);
            } else {
                setStatus("❌ " + quem + " errou. Próxima pergunta em breve...", COR_VERMELHO);
            }
        }
    }

    private void tratarFimDeJogo(Partida p) {
        pararTimer();
        int venc = p.vencedor();

        String nomeVenc = venc == -1 ? "" : p.getNome(venc);

        if (venc == -1) {
            lblResultadoFinal.setText("🤝 Empate!");
            lblResultadoFinal.setForeground(COR_AMARELO);
        } else if (venc == numeroJogador) {
            lblResultadoFinal.setText("🏆 Você venceu, " + nomeJogador + "!");
            lblResultadoFinal.setForeground(COR_VERDE);
        } else {
            lblResultadoFinal.setText("😔 " + nomeVenc + " venceu!");
            lblResultadoFinal.setForeground(COR_VERMELHO);
        }

        lblPlacarFinal.setText(p.getNome(0) + ": " + p.getPontos(0)
                + " pts   |   " + p.getNome(1) + ": " + p.getPontos(1) + " pts");

        // Mostra placar anterior na tela de espera ao reiniciar
        lblEsperaSubtitulo.setText("Último placar — " + p.getNome(0) + ": " + p.getPontos(0)
                + "  |  " + p.getNome(1) + ": " + p.getPontos(1));

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
        ultimaRespostaEnviada = indice;
        desabilitarAlternativas();
        botoesAlternativas[indice].setBackground(COR_DESTAQUE);
        enviarMensagem(new Mensagem(Mensagem.Tipo.RESPOSTA, indice));
        setStatus("✉️ Resposta enviada! Aguardando resultado...", COR_TEXTO_FRACO);
    }

    private void reiniciar() {
        btnReiniciar.setEnabled(false);
        lblPlacarFinal.setText("Aguardando adversário confirmar...");
        enviarMensagem(new Mensagem(Mensagem.Tipo.REINICIAR));
    }

    // =========================================================================
    // Controles de UI
    // =========================================================================

    private void liberarBuzzer() {
        pararTimer();
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
        lblPontosEu.setText(String.valueOf(p.getPontos(numeroJogador)));
        lblPontosAdv.setText(String.valueOf(p.getPontos(1 - numeroJogador)));
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
        lblTimer.setForeground(COR_AMARELO);
    }

    private void setStatus(String texto, Color cor) {
        lblStatus.setText(texto);
        lblStatus.setForeground(cor);
    }

    private void mostrarTela(String nome) {
        cardLayout.show(painelPrincipal, nome);
    }

    // =========================================================================
    // Comunicação
    // =========================================================================

    private synchronized void enviarMensagem(Mensagem m) {
        try {
            output.writeObject(m);
            output.flush();
            output.reset();
        } catch (IOException e) {
            setStatus("Erro ao enviar: " + e.getMessage(), COR_VERMELHO);
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
