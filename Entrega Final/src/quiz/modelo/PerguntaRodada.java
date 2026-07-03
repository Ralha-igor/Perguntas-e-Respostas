package quiz.modelo;

import java.io.Serializable;

/**
 * Agrupa uma Pergunta com o número da rodada atual e o total de rodadas.
 * Enviado pelo servidor ao cliente via NOVA_PERGUNTA para que o display
 * de rodada seja sempre correto, sem depender do estado local do cliente.
 */
public class PerguntaRodada implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Pergunta pergunta;
    private final int rodadaAtual;   // 1-based (já somado +1 pelo servidor)
    private final int totalRodadas;

    public PerguntaRodada(Pergunta pergunta, int rodadaAtual, int totalRodadas) {
        this.pergunta     = pergunta;
        this.rodadaAtual  = rodadaAtual;
        this.totalRodadas = totalRodadas;
    }

    public Pergunta getPergunta()    { return pergunta; }
    public int getRodadaAtual()      { return rodadaAtual; }
    public int getTotalRodadas()     { return totalRodadas; }
}
