package quiz.modelo;

import java.io.Serializable;
import java.util.List;

/**
 * Representa o estado de uma partida.
 * Enviada pelo servidor ao cliente para sincronizar placar e progresso.
 */
public class Partida implements Serializable {

    private static final long serialVersionUID = 1L;

    private int rodadaAtual;
    private final int totalRodadas;
    private int[] pontos; // pontos[0] = J1, pontos[1] = J2

    public Partida(int totalRodadas) {
        this.totalRodadas = totalRodadas;
        this.rodadaAtual = 0;
        this.pontos = new int[]{0, 0};
    }

    public void adicionarPonto(int jogador) {
        if (jogador == 0 || jogador == 1)
            pontos[jogador]++;
    }

    public void proximaRodada() { rodadaAtual++; }

    public boolean finalizada() { return rodadaAtual >= totalRodadas; }

    public int getRodadaAtual() { return rodadaAtual; }
    public int getTotalRodadas() { return totalRodadas; }
    public int getPontos(int jogador) { return pontos[jogador]; }
    public int[] getTodosPontos() { return pontos; }

    public int vencedor() {
        if (pontos[0] > pontos[1]) return 0;
        if (pontos[1] > pontos[0]) return 1;
        return -1; // empate
    }
}