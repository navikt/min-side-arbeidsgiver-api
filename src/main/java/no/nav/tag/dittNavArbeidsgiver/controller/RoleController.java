package no.nav.tag.dittNavArbeidsgiver.controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoleController {
    @RequestMapping(value = "/api/Role")
    public @ResponseBody
    String getItem(@RequestBody Body body)  {
        return body.testVerdi;
    }
  /*  public Role returnRole() {
        Role newRole = new Role();
        newRole.setRolleDefinisjonId(2);
        newRole.setRolleId(499);
        newRole.setRolleNavn("Guro");
        newRole.setRolleType("sjef");
        System.out.println("hei");
        return newRole;
    } */


}

