package software.netcore.radman.data.radius.repo;

import org.springframework.data.repository.CrudRepository;
import software.netcore.radman.data.radius.entity.RadGroupReply;

import java.util.List;

/**
 * @since v. 1.0.0
 */
public interface RadGroupReplyRepo extends CrudRepository<RadGroupReply, Integer> {

    List<RadGroupReply> findAll();

}
