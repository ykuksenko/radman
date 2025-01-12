package software.netcore.radman.ui.view;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.RequiredArgsConstructor;

/**
 * @since v. 1.0.0
 */
@PageTitle("Logout")
@Route(value = "logout")
@RequiredArgsConstructor
public class LogoutView extends VerticalLayout {
}
