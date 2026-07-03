package quiz.modelo;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Evento: jogador errou a resposta. Perde a vez, não perde ponto. */
public class EventoErro extends EventoJogo {

    public EventoErro(int jogador, Partida partida, int indiceCorreto,
                      BiConsumer<Integer, Integer> fnAlterarPonto,
                      Consumer<Mensagem> fnBroadcast) {
        super(jogador, partida, indiceCorreto, fnAlterarPonto, fnBroadcast);
    }

    @Override
    public void aplicar() {
        // O adversário ainda vai ter a segunda chance nesta rodada,
        // então o índice correto NÃO pode ser revelado ainda.
        broadcast(new Mensagem(Mensagem.Tipo.RESULTADO_RODADA, payloadResultado(false, false)));
    }
}