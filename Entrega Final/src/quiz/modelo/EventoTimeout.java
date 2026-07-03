package quiz.modelo;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Evento: jogador não respondeu a tempo. Perde a vez, não perde ponto. */
public class EventoTimeout extends EventoJogo {

    public EventoTimeout(int jogador, Partida partida, int indiceCorreto,
                         BiConsumer<Integer, Integer> fnAlterarPonto,
                         Consumer<Mensagem> fnBroadcast) {
        super(jogador, partida, indiceCorreto, fnAlterarPonto, fnBroadcast);
    }

    @Override
    public void aplicar() {
        broadcast(new Mensagem(Mensagem.Tipo.RESULTADO_RODADA, payloadResultado(false)));
    }
}
