package software.netcore.radman.buisness.service.user.radius.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @since v. 1.0.0
 */
@Getter
@Setter
@NoArgsConstructor
public class RadiusGroupDto {

    private Long id;
    private String name;
    private String description;

}
