package quiz.modelo;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Evento: jogador respondeu corretamente. Adiciona 1 ponto. */
public class EventoAcerto extends EventoJogo {

    public EventoAcerto(int jogador, Partida partida, int indiceCorreto,
                        BiConsumer<Integer, Integer> fnAlterarPonto,
                        Consumer<Mensagem> fnBroadcast) {
        super(jogador, partida, indiceCorreto, fnAlterarPonto, fnBroadcast);
    }

    @Override
    public void aplicar() {
        adicionarPonto(jogador);
        broadcast(new Mensagem(Mensagem.Tipo.RESULTADO_RODADA, payloadResultado(true)));
    }
}
