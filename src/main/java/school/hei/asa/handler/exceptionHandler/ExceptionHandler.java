package school.hei.asa.handler.exceptionHandler;

import school.hei.asa.PojaGenerated;

@PojaGenerated
public interface ExceptionHandler<R> {
  R handle(Throwable throwable);
}
