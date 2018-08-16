package cc.dmji.api.repository.v2;

import cc.dmji.api.entity.v2.MessageV2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;

public interface MessageV2Repository extends JpaRepository<MessageV2, Long> {
    List<MessageV2> findByUidEqualsAndTypeEqualsAndReadEquals(Long uid, Integer messageType, boolean isRead);
    Long countByUidEqualsAndTypeEqualsAndReadEquals(Long uid, Integer messageType, boolean isRead);
}
