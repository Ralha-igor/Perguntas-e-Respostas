package quiz.modelo;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Representa um evento ocorrido durante uma rodada do quiz.
 *
 * Demonstra os pilares de POO:
 *  - Abstração   : define a estrutura comum de qualquer evento de rodada
 *  - Herança     : subclasses especializam o comportamento
 *  - Polimorfismo: o Servidor chama evento.aplicar() sem saber qual tipo concreto é
 *  - Encapsulamento: dados protegidos com getters
 */
public abstract class EventoJogo {

    protected final int jogador;
    protected final Partida partida;
    protected final int indiceCorreto; // -1 quando não aplicável

    private final BiConsumer<Integer, Integer> fnAlterarPonto;
    private final Consumer<Mensagem>           fnBroadcast;

    protected EventoJogo(int jogador, Partida partida, int indiceCorreto,
                         BiConsumer<Integer, Integer> fnAlterarPonto,
                         Consumer<Mensagem> fnBroadcast) {
        this.jogador        = jogador;
        this.partida        = partida;
        this.indiceCorreto  = indiceCorreto;
        this.fnAlterarPonto = fnAlterarPonto;
        this.fnBroadcast    = fnBroadcast;
    }

    /** Aplica o evento: atualiza pontuação e notifica os clientes. */
    public abstract void aplicar();

    protected void adicionarPonto(int j) { fnAlterarPonto.accept(j, +1); }
    protected void removerPonto(int j)   { fnAlterarPonto.accept(j, -1); }
    protected void broadcast(Mensagem m) { fnBroadcast.accept(m); }

    /**
     * Payload enviado ao cliente com o resultado da rodada.
     * [0] jogador respondente (-1 = ninguém)
     * [1] acertou (boolean)
     * [2] indiceCorreto (int) — cliente usa para destacar a alternativa certa
     * [3] partida clonada com placar atualizado
     */
    protected Object[] payloadResultado(boolean acertou) {
        return new Object[]{jogador, acertou, indiceCorreto, clonar(partida)};
    }

    private Partida clonar(Partida p) {
        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            new java.io.ObjectOutputStream(bos).writeObject(p);
            return (Partida) new java.io.ObjectInputStream(
                    new java.io.ByteArrayInputStream(bos.toByteArray())).readObject();
        } catch (Exception e) { return p; }
    }

    public int getJogador() { return jogador; }
}
