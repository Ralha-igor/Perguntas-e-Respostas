package quiz.modelo;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Evento: ninguém clicou no buzzer. Sem alteração de pontos. */
public class EventoSemResposta extends EventoJogo {

    public EventoSemResposta(Partida partida, int indiceCorreto,
                             BiConsumer<Integer, Integer> fnAlterarPonto,
                             Consumer<Mensagem> fnBroadcast) {
        super(-1, partida, indiceCorreto, fnAlterarPonto, fnBroadcast);
    }

    @Override
    public void aplicar() {
        broadcast(new Mensagem(Mensagem.Tipo.RESULTADO_RODADA, payloadResultado(false)));
    }
}
