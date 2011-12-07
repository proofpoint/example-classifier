package com.proofpoint.example.classifier;

import com.google.common.collect.ImmutableSet;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/v1/usersOnHold")
public class UsersOnHoldResource
{
    private final UsersOnHold usersOnHold;

    @Inject
    public UsersOnHoldResource(UsersOnHold usersOnHold)
    {
        this.usersOnHold = usersOnHold;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void replaceUsersOnHold(List<String> userIds)
    {
        usersOnHold.replace(ImmutableSet.copyOf(userIds));
    }
}
