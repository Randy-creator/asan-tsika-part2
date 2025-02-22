package school.hei.asa.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class MissionTest {

  @Test
  void mission_has_0_executed_days_when_no_worker() {
    var product = new Product("pcode", "pname", "pdescription");
    var mission = new Mission("mission-code", "Titre", "Description", 10, product);
    assertEquals(0, mission.executedDays());
  }
}
