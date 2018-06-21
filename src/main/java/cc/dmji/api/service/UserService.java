package cc.dmji.api.service;

import cc.dmji.api.entity.User;
import org.springframework.data.domain.Page;

import java.util.Date;
import java.util.List;

/**
 * Created by echisan on 2018/5/14
 */
public interface UserService {

    User insertUser(User user);

    void deleteUserById(String id);

    User deleteUserByNick(String nick);

    User deleteUserByEmail(String email);

    User updateUser(User user);

    User getUserById(String id);

    User getUserByNick(String nick);

    User getUserByEmail(String email);

    List<User> listUser(Integer pn, Integer ps);

    List<User> listUser();

    Long countUsers();

    List<User> listUsersNickLike(String nick, Integer pn, Integer ps);

    Long countUsersNickLike(String nick);

    Long countUsersByCreateTime(Date begin, Date end);

}
