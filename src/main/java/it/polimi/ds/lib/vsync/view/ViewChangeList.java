package it.polimi.ds.lib.vsync.view;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * This class helps with tracking the list of host during the view change operations:
 * it should be created with an expected list of devices that should connect, or with a
 * single users that try to connect with the host while unexpected
 * <p>
 * When a list has been created with a waited ysers list, use {@link ViewChangeList#addConnectedUser}
 * to add all the users that progressively connect with it.
 * <p>
 * When a list has been created before setting a waited users list, use {@link ViewChangeList#setExpectedUsers}
 * to create it and then add connected users as before.
 */
public class ViewChangeList {
    private final List<UUID> expectedUsers;
    private final List<UUID> connectedUsers;
    private boolean expectedUsersSetted = false;

    private ViewChangeList(List<UUID> expectedUsers, List<UUID> connectedUsers) {
        this.expectedUsers = expectedUsers;
        this.expectedUsersSetted = !expectedUsers.isEmpty();
        this.connectedUsers = connectedUsers;
    }

    public static ViewChangeList fromExpectedUsers(List<UUID> expectedUsers) {
        return new ViewChangeList(new ArrayList<>(expectedUsers), new ArrayList<>());
    }

    public static ViewChangeList fromUnexpectedUser(UUID unexpectedUser) {
        final List<UUID> temp = new ArrayList<>();
        temp.add(unexpectedUser);
        return new ViewChangeList(new ArrayList<>(), temp);
    }

    synchronized public boolean isComplete() {
        return new HashSet<>(connectedUsers).equals(new HashSet<>(expectedUsers));
    }

    synchronized public void addConnectedUser(UUID newUserId) {
        connectedUsers.add(newUserId);
    }

    synchronized public void setExpectedUsers(List<UUID> users) {
        for (UUID user : users) {
            if (!connectedUsers.remove(user)) {
                expectedUsers.add(user);
            }
        }
        expectedUsersSetted = true;
    }
}
