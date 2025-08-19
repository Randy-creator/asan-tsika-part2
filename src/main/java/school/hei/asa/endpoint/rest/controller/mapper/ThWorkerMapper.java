package school.hei.asa.endpoint.rest.controller.mapper;

import static java.time.Instant.now;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import school.hei.asa.CareProductCodeSupplier;
import school.hei.asa.endpoint.rest.model.th.ThWorkerLevelHistory;
import school.hei.asa.model.MissionExecution;
import school.hei.asa.model.Worker;
import school.hei.asa.model.WorkerLevelHistory;
import school.hei.asa.repository.MissionExecutionRepository;

@Slf4j
@AllArgsConstructor
@Component
public class ThWorkerMapper {

  private final MissionExecutionRepository missionExecutionRepository;
  private final CareProductCodeSupplier careProductCodeSupplier;

  @Transactional(readOnly = true)
  public List<ThWorkerLevelHistory> toTh(List<WorkerLevelHistory> histories) {
    if (histories.isEmpty()) {
      return new ArrayList<>();
    }
    ZoneId zoneId = ZoneId.of("UTC");
    Worker worker = histories.getFirst().worker();
    LocalDate startDate = histories.getLast().entranceInstant().atZone(zoneId).toLocalDate();
    LocalDate endDate = now().atZone(zoneId).toLocalDate();

    List<MissionExecution> missionExecutions =
        missionExecutionRepository
            .missionExecutionsByDateBetween(worker, startDate, endDate)
            .parallelStream()
            .filter(me -> !isCare(me))
            .toList();

    return histories.stream()
        .map(
            current -> {
              var currentIndex = histories.indexOf(current);
              var nextEntrance =
                  (currentIndex == 0) ? now() : histories.get(currentIndex - 1).entranceInstant();

              double totalDaysWorked =
                  missionExecutions.parallelStream()
                      .filter(
                          me -> {
                            return me.date()
                                    .isAfter(current.entranceInstant().atZone(zoneId).toLocalDate())
                                && me.date().isBefore(nextEntrance.atZone(zoneId).toLocalDate());
                          })
                      .mapToDouble(MissionExecution::dayPercentage)
                      .sum();

              var contractType = toWorkerType(current.contractType());
              var totalWorkDays = Objects.toString(current.projectedDaysToWork(), "-");
              return new ThWorkerLevelHistory(
                  current.level().getLevel(),
                  current.entranceInstant(),
                  contractType,
                  totalWorkDays,
                  String.valueOf(totalDaysWorked),
                  current.salary(),
                  current.jobTitle(),
                  current.contractDuration());
            })
        .toList();
  }

  public String toWorkerType(String contractType) {
    return switch (contractType) {
      case "partnerContractor" -> "Prestataire";
      case "fullTimeEmployee" -> "Salarié";
      case null -> "";
      default -> "Alternant";
    };
  }

  private boolean isCare(MissionExecution me) {
    var mission = me.mission();
    return mission.isCare(careProductCodeSupplier.get());
  }
}
