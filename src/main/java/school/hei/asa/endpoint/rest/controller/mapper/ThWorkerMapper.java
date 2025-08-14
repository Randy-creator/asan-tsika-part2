package school.hei.asa.endpoint.rest.controller.mapper;

import static java.time.Instant.now;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import school.hei.asa.CareProductCodeSupplier;
import school.hei.asa.endpoint.rest.model.th.ThWorkerLevelHistory;
import school.hei.asa.model.MissionExecution;
import school.hei.asa.model.Worker;
import school.hei.asa.model.WorkerLevelHistory;
import school.hei.asa.repository.MissionExecutionRepository;

@AllArgsConstructor
@Component
public class ThWorkerMapper {

  private final MissionExecutionRepository missionExecutionRepository;
  private final CareProductCodeSupplier careProductCodeSupplier;

  public List<ThWorkerLevelHistory> toTh(List<WorkerLevelHistory> histories) {
    if (histories == null || histories.isEmpty()) {
      return new ArrayList<>();
    }

    final ZoneId UTC_ZONE = ZoneId.of("UTC");
    final Instant currentTime = now();
    final List<ThWorkerLevelHistory> result = new ArrayList<>(histories.size());

    for (int i = 0; i < histories.size(); i++) {
      final WorkerLevelHistory current = histories.get(i);
      final Instant nextEntrance = (i == 0) ? currentTime : histories.get(i - 1).entranceInstant();

      final LocalDate entranceDate = current.entranceInstant().atZone(UTC_ZONE).toLocalDate();
      final LocalDate nextEntranceDate = nextEntrance.atZone(UTC_ZONE).toLocalDate();

      final double totalDaysWorked =
          missionExecutionPercentageSumByWorker(current.worker(), entranceDate, nextEntranceDate);

      result.add(
          new ThWorkerLevelHistory(
              current.level().getLevel(),
              current.entranceInstant(),
              toWorkerType(current.contractType()),
              Objects.toString(current.projectedDaysToWork(), "-"),
              String.valueOf(totalDaysWorked),
              current.salary(),
              current.jobTitle(),
              current.contractDuration()));
    }

    return result;
  }

  public String toWorkerType(String contractType) {
    return switch (contractType) {
      case "partnerContractor" -> "Prestataire";
      case "fullTimeEmployee" -> "Salarié";
      case null -> "";
      default -> "Alternant";
    };
  }

  private Double missionExecutionPercentageSumByWorker(
      Worker worker, LocalDate startDate, LocalDate endDate) {
    return missionExecutionRepository
        .missionExecutionsByDateBetween(worker, startDate, endDate)
        .stream()
        .filter(me -> !isCare(me))
        .mapToDouble(MissionExecution::dayPercentage)
        .sum();
  }

  private boolean isCare(MissionExecution me) {
    var mission = me.mission();
    return mission.isCare(careProductCodeSupplier.get());
  }
}
