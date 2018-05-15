package cc.dmji.api.repository;

import cc.dmji.api.entity.Bangumi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.regex.Matcher;


public interface BangumiRepository extends JpaRepository<Bangumi,Integer> {

    List<Bangumi> findBangumisByBangumiNameLike(String name);

}
