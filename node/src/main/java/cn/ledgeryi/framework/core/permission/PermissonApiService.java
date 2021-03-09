package cn.ledgeryi.framework.core.permission;

import cn.ledgeryi.framework.core.permission.entity.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j(topic = "permission")
@Service
public class PermissonApiService {

    public void addRole(String roleId){
        //add a role into contract
        Role role = Role.builder().roleId(roleId).build();

    }

    public void inactiveRole(String roleId){
        //inactive a role
    }

    public void assignRoleForAccount(String account){

    }

    public void revocationRoleOfAccount(String roleId, String account){

    }

    public boolean hasRole(String roleId, String account){
        return true;
    }

    public void addNode(String nodeOwner, String netAddress){

    }

    public void approvedNode(String nodeOwner, String netAddress){

    }

    public void UpdateNodeStatus(String nodeOwner, String netAddress){

    }
}
