/*
 * Copyright 2016, Sascha Häberling
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.retrostore.data.user;

import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;

import java.util.List;
import java.util.Optional;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Functionality to manage users.
 */
public class UserManagement {

  private final UserService userService;

  public UserManagement(UserService userService) {
    this.userService = userService;
  }

  /**
   * @return Whether the currently logged in user is an admin of the TRS80 app store.
   */
  public boolean isCurrentUserAdmin() {
    Optional<String> loggedInEmail = getLoggedInEmail();
    if (!loggedInEmail.isPresent()) {
      return false;
    }
    Optional<RetroStoreUser> user = getUserByEmail(loggedInEmail.get());
    return user.filter(retroStoreUser -> retroStoreUser.type == UserAccountType.ADMIN).isPresent();
  }

  /**
   * @return The e-mail address of the currently logged in user.
   */
  public Optional<String> getLoggedInEmail() {
    User currentUser = userService.getCurrentUser();
    if (currentUser == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(currentUser.getEmail());
  }

  /**
   * @return Whether an admin exists in the system.
   */
  public boolean hasAdmin() {
    List<RetroStoreUser> users = ofy().load().type(RetroStoreUser.class).list();
    for (RetroStoreUser user : users) {
      if (user.type == UserAccountType.ADMIN) {
        return true;
      }
    }
    return false;
  }

  /**
   * Adds a new user if one with the given ID does not exist yet, otherwise changes the existing
   * user with the given ID.
   */
  public void addOrChangeUser(RetroStoreUser user) {
    ofy().save().entity(user).now();
  }

  /**
   * Remote the user with the given email.
   *
   * @param email the email of the user to delete.
   */
  public void removeUser(String email) {
    ofy().delete().key(RetroStoreUser.key(email)).now();
  }

  /**
   * @return A list of all users in the system.
   */
  public List<RetroStoreUser> getAllUsers() {
    return ofy().load().type(RetroStoreUser.class).list();
  }

  /**
   * If it exists in the system, returns the user with the given email address.
   */
  public Optional<RetroStoreUser> getUserByEmail(String email) {
    return Optional.ofNullable(ofy().load().key(RetroStoreUser.key(email)).now());
  }

  public Optional<RetroStoreUser> getCurrentUser() {
    User systemUser = userService.getCurrentUser();
    if (systemUser == null) {
      return Optional.empty();
    }
    return getUserByEmail(systemUser.getEmail());
  }
}
