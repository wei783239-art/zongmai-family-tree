package com.zongmai.familytree;


import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestParam;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/families")
@PreAuthorize("denyAll()")
public class FamilyController {

    private final FamilyService familyService;
    private final PersonService personService;
    private final RelationService relationService;

    public FamilyController(FamilyService familyService, PersonService personService, RelationService relationService) {
        this.familyService = familyService;
        this.personService = personService;
        this.relationService = relationService;
    }

    
    @GetMapping
    @PreAuthorize("@familyAccess.isActive(authentication)")
    public List<Map<String, Object>> listFamilies(@AuthenticationPrincipal LoginUser user) {

        return familyService.listFamilies(user.id());

    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("@familyAccess.isActive(authentication)")
    public Map<String, Object> createFamily (
        @AuthenticationPrincipal LoginUser user,
        @Valid @RequestBody FamilyRequest request) {

        Long familyId = familyService.createFamily(user.id(), request);

        return Map.of("id", familyId, "message", "家族创建成功");
    }

    @PreAuthorize("@familyAccess.canRead(#familyId, authentication)")
    @GetMapping("/{familyId}/persons")
    public List<Map<String, Object>> listPersonsByFamily(
            @PathVariable("familyId") Long familyId,
            @RequestParam(value = "name", required = false) String name) {
   
    return personService.listPersonsByFamily(familyId, name);
    }   


    @PreAuthorize("@familyAccess.canManage(#familyId, authentication)")
    @PostMapping("/{familyId}/persons")
    public Map<String, String> createPerson(
            @PathVariable("familyId") Long familyId,
            @Valid @RequestBody PersonRequest request) {
    
        personService.createPerson(familyId, request);

        return Map.of("message", "成员添加成功");
    }

    @PreAuthorize("@familyAccess.canManage(#familyId, authentication)")
    @PutMapping("/{familyId}/persons/{personId}")
    public Map<String, String> updatePerson(
            @PathVariable("familyId") Long familyId,
            @PathVariable("personId") Long personId,
            @Valid @RequestBody PersonRequest request) {
        
                personService.updatePerson(familyId, personId, request);

                return Map.of("message", "成员信息已更新");
    }

    @PreAuthorize("@familyAccess.canManage(#familyId, authentication)")
    @DeleteMapping("/{familyId}/persons/{personId}")
    public Map<String, String> deletePerson(
            @PathVariable("familyId") Long familyId,
            @PathVariable("personId") Long personId) {
        personService.deletePerson(familyId, personId);

        return Map.of("message", "成员已删除");
    }

    @PreAuthorize("@familyAccess.canRead(#familyId, authentication)")
    @GetMapping("/{familyId}/relations")
    public List<Map<String, Object>> listRelationsByFamily(
            @PathVariable("familyId") Long familyId) {
        
        return relationService.listRelationsByFamily(familyId);
    }

    @PreAuthorize("@familyAccess.canManage(#familyId, authentication)")
    @PostMapping("/{familyId}/relations")
    public Map<String, String> createParentChildRelation(
            @PathVariable("familyId") Long familyId,
            @Valid @RequestBody ParentChildRelationRequset request) {
        
        relationService.createParentChildRelation(familyId, request);

        return Map.of("message", "亲子关系添加成功");
    }

    @PreAuthorize("@familyAccess.canManage(#familyId, authentication)")
    @DeleteMapping("/{familyId}/relations/{relationId}")
        public Map<String, String> deleteRelation(
                @PathVariable("familyId") Long familyId,
                @PathVariable("relationId") Long relationId) {
            
            relationService.deleteRelation(familyId, relationId);
                
            return Map.of("message", "亲属关系已删除");
    }

    @PreAuthorize("@familyAccess.canRead(#familyId, authentication)")
    @GetMapping("/{familyId}/persons/{personId}")
        public Map<String, Object> getPerson(
                @PathVariable("familyId") Long familyId,
                @PathVariable("personId") Long personId) {
        
          return personService.getPerson(familyId, personId);
        }

        
    

    @PreAuthorize("@familyAccess.canManage(#familyId, authentication)")
    @PutMapping("/{familyId}")
    public Map<String, String> updateFamily(
            @PathVariable("familyId") Long familyId,
            @Valid @RequestBody FamilyRequest request) {
        
        familyService.updateFamily(familyId, request);

        return Map.of("message", "家族名称已更新"); 
    }

    @PreAuthorize("@familyAccess.canManage(#familyId, authentication)")
    @DeleteMapping("/{familyId}")
    public Map<String, String> deleteFamliy(
            @PathVariable("familyId") Long familyId) {
        
                familyService.deleteFamily(familyId);

                return Map.of("message", "家族已删除");
    }


    @PreAuthorize("@familyAccess.canRead(#familyId, authentication)")
    @GetMapping("/{familyId}/persons/page")
    public Map<String, Object> pagePersonsByFamliy(
                        @PathVariable("familyId") Long familyId,
                        @RequestParam(value = "page", defaultValue = "1") int page,
                        @RequestParam(value = "size", defaultValue = "10") int size,
                        @RequestParam(value = "name", required = false) String name,
                        @RequestParam(value = "generation", required = false) Integer generation,
                        @RequestParam(value = "birthYear", required = false) Integer birthYear) {

                return personService.pagePersonsByFamily(familyId, page, size, name,generation,birthYear);
    }

    @PreAuthorize("@familyAccess.canRead(#familyId, authentication)")
    @GetMapping("/{familyId}")
    public  Map<String, Object> getFamily(
        @PathVariable("familyId") Long familyId) {

    return familyService.getFamily(familyId);
    }

    @PreAuthorize("@familyAccess.canManage(#familyId, authentication)")
    @PutMapping("/{familyId}/ancestor")
    public Map<String, String> setAncestor(
            @PathVariable("familyId") Long familyId,
            @Valid @RequestBody AncestorRequest request) {

        familyService.setAncestor(familyId, request);

        return Map.of("message", "家族始祖已更新");
    }

    @PreAuthorize("@familyAccess.canManage(#familyId, authentication)")
    @DeleteMapping("/{familyId}/ancestor")
    public Map<String, String> clearAncestor(
            @PathVariable("familyId") Long familyId) {

        familyService.clearAncestor(familyId);

        return Map.of("message", "家族始祖指定已取消");
    }

    @PreAuthorize("@familyAccess.canRead(#familyId, authentication)")
    @GetMapping("/{familyId}/persons/{personId}/descendants")
    public List<Map<String, Object>> listDescendants(
            @PathVariable("familyId") Long familyId,
            @PathVariable("personId") Long personId) {

        return personService.listDescendants(familyId, personId);
    }

    @PreAuthorize("@familyAccess.canRead(#familyId, authentication)")
    @GetMapping("/{familyId}/persons/{personId}/ancestors")
    public List<Map<String, Object>> listAncestors(
            @PathVariable("familyId") Long familyId,
            @PathVariable("personId") Long personId) {

        return personService.listAncestors(familyId, personId);
    }

    @PreAuthorize("@familyAccess.canManage(#familyId, authentication)")
    @PostMapping("/{familyId}/relations/spouse")
    public Map<String, String> createSpouseRelation(
            @PathVariable("familyId") Long familyId,
            @Valid @RequestBody SpouseRelationRequest request) {
                
        relationService.createSpouseRelation(familyId, request);

        return  Map.of("message", "配偶关系添加成功");
    }
}
