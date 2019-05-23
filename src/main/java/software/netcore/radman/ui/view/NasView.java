package software.netcore.radman.ui.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.validator.IntegerRangeValidator;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.function.ValueProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.vaadin.artur.spring.dataprovider.SpringDataProviderBuilder;
import software.netcore.radman.buisness.service.nas.NasService;
import software.netcore.radman.buisness.service.nas.dto.NasDto;
import software.netcore.radman.ui.CreationListener;
import software.netcore.radman.ui.UpdateListener;
import software.netcore.radman.ui.component.ConfirmationDialog;
import software.netcore.radman.ui.converter.DoubleToIntegerConverter;
import software.netcore.radman.ui.menu.MainTemplate;
import software.netcore.radman.ui.notification.ErrorNotification;

import java.util.Objects;

/**
 * @since v. 1.0.0
 */
@Slf4j
@PageTitle("Radman: NAS")
@Route(value = "", layout = MainTemplate.class)
public class NasView extends Div {

    private final NasService nasService;

    @Autowired
    public NasView(NasService nasService) {
        this.nasService = nasService;
        buildView();
    }

    private void buildView() {
        Grid<NasDto> grid = new Grid<>(NasDto.class, false);
        grid.addColumns("nasName", "shortName", "description");
        grid.addColumn((ValueProvider<NasDto, String>) nasDto
                -> nasDto.getSecret().replaceAll(".", "*")).setHeader("Secret");
        grid.addColumns("server", "community", "ports", "type");
        DataProvider<NasDto, Object> dataProvider = new SpringDataProviderBuilder<>(
                (pageable, o) -> nasService.pageNasRecords(pageable), value -> nasService.countNasRecords())
                .withDefaultSort("id", SortDirection.ASCENDING)
                .build();
        grid.setDataProvider(dataProvider);
        grid.getColumns().forEach(column -> column.setResizable(true));
        grid.setColumnReorderingAllowed(true);

        ConfirmationDialog nasDeleteDialog = new ConfirmationDialog("340px");
        nasDeleteDialog.setTitle("Delete NAS");
        nasDeleteDialog.setConfirmButtonCaption("Delete");
        nasDeleteDialog.setConfirmListener(() -> {
            NasDto nasDto = grid.getSelectionModel().getFirstSelectedItem().orElse(null);
            if (Objects.nonNull(nasDto)) {
                try {
                    nasService.deleteNas(nasDto);
                    grid.getDataProvider().refreshAll();
                } catch (Exception e) {
                    log.warn("Failed to delete NAS. Reason = '{}'", e.getMessage());
                    ErrorNotification.show("Error",
                            "Ooops, something went wrong, try again please");
                }
            }
            nasDeleteDialog.setOpened(false);

        });

        NasEditDialog nasEditDialog = new NasEditDialog(nasService,
                (source, bean) -> grid.getDataProvider().refreshItem(bean));
        NasCreateDialog nasCreateDialog = new NasCreateDialog(nasService,
                (source, bean) -> grid.getDataProvider().refreshAll());

        Button createBtn = new Button("Create", event -> nasCreateDialog.startNasCreation());
        Button editBtn = new Button("Edit", event -> {
            NasDto nasDto = grid.getSelectionModel().getFirstSelectedItem().orElse(null);
            if (Objects.nonNull(nasDto)) {
                nasEditDialog.editNas(nasDto);
            }
        });
        Button deleteBtn = new Button("Delete", event -> {
            NasDto nasDto = grid.getSelectionModel().getFirstSelectedItem().orElse(null);
            if (Objects.nonNull(nasDto)) {
                nasDeleteDialog.setDescription("Are you sure you want to remove '" + nasDto.getNasName() + "' NAS?");
                nasDeleteDialog.setOpened(true);
            }
        });

        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.BASELINE);
        horizontalLayout.add(new H3("NAS"));
        horizontalLayout.add(createBtn);
        horizontalLayout.add(editBtn);
        horizontalLayout.add(deleteBtn);
        add(horizontalLayout);
        add(grid);
    }

    static class NasCreateDialog extends NasFormDialog {

        private final CreationListener<NasDto> creationListener;

        NasCreateDialog(NasService nasService,
                        CreationListener<NasDto> creationListener) {
            super(nasService);
            this.creationListener = creationListener;
        }

        @Override
        String getDialogTitle() {
            return "Create NAS";
        }

        @Override
        Button getConfirmBtn() {
            return new Button("Create", event -> {
                NasDto nasDto = new NasDto();
                if (binder.writeBeanIfValid(nasDto)) {
                    try {
                        nasDto = nasService.createNas(nasDto);
                        creationListener.onCreated(this, nasDto);
                    } catch (Exception e) {
                        log.warn("Failed to create NAS. Reason = '{}'", e.getMessage());
                        ErrorNotification.show("Error",
                                "Ooops, something went wrong, try again please");
                    }
                    setOpened(false);
                }
            });
        }

        void startNasCreation() {
            setOpened(true);
            binder.readBean(new NasDto());
        }

    }

    static class NasEditDialog extends NasFormDialog {

        private final UpdateListener<NasDto> updateListener;

        NasEditDialog(NasService nasService,
                      UpdateListener<NasDto> updateListener) {
            super(nasService);
            this.updateListener = updateListener;
        }

        @Override
        String getDialogTitle() {
            return "Edit NAS";
        }

        @Override
        Button getConfirmBtn() {
            return new Button("Save", event -> {
                if (binder.isValid()) {
                    try {
                        NasDto dto = nasService.updateNas(binder.getBean());
                        updateListener.onUpdated(this, dto);
                    } catch (Exception e) {
                        log.warn("Failed to update NAS. Reason = '{}'", e.getMessage());
                        ErrorNotification.show("Error",
                                "Ooops, something went wrong, try again please");
                    }
                    setOpened(false);
                }
            });
        }

        void editNas(NasDto dto) {
            setOpened(true);
            binder.setBean(dto);
        }

    }

    static abstract class NasFormDialog extends Dialog {

        final NasService nasService;
        final Binder<NasDto> binder;

        NasFormDialog(NasService nasService) {
            this.nasService = nasService;

            add(new H3(getDialogTitle()));
            TextField name = new TextField("Name");
            name.setValueChangeMode(ValueChangeMode.EAGER);
            TextField shortName = new TextField("Short name");
            shortName.setValueChangeMode(ValueChangeMode.EAGER);
            TextField type = new TextField("Type");
            type.setValueChangeMode(ValueChangeMode.EAGER);
            NumberField port = new NumberField("Port");
            port.setValueChangeMode(ValueChangeMode.EAGER);
            PasswordField secret = new PasswordField("Secret");
            secret.setValueChangeMode(ValueChangeMode.EAGER);
            TextField server = new TextField("Server");
            server.setValueChangeMode(ValueChangeMode.EAGER);
            TextField community = new TextField("Community");
            community.setValueChangeMode(ValueChangeMode.EAGER);
            TextArea description = new TextArea("Description");
            description.setValueChangeMode(ValueChangeMode.EAGER);

            FormLayout formLayout = new FormLayout();
            formLayout.setWidthFull();
            formLayout.setMaxWidth("700px");
            formLayout.add(name, shortName, server, port, secret, type, community, description);
            formLayout.setResponsiveSteps(
                    new FormLayout.ResponsiveStep("0px", 1),
                    new FormLayout.ResponsiveStep("450px", 2));

            HorizontalLayout controlsLayout = new HorizontalLayout();
            controlsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
            controlsLayout.add(new Button("Cancel", event -> setOpened(false)));
            controlsLayout.add(getConfirmBtn());
            controlsLayout.setWidthFull();

            add(formLayout);
            add(new Hr());
            add(controlsLayout);

            binder = new Binder<>(NasDto.class);
            binder.forField(name)
                    .asRequired("Name is required.")
                    .withValidator(new StringLengthValidator("Name has to contain 2, " +
                            "up to 128 characters.", 2, 128))
                    .bind(NasDto::getNasName, NasDto::setNasName);
            binder.forField(shortName)
                    .bind(NasDto::getShortName, NasDto::setShortName);
            binder.forField(type)
                    .bind(NasDto::getType, NasDto::setType);
            binder.forField(port)
                    .withConverter(new DoubleToIntegerConverter("Port must be number " +
                            "between 1 and " + 65535 + "."))
                    .withValidator(new IntegerRangeValidator("Port must be number " +
                            "between 1 and " + 65535 + ".", 1, 65535))
                    .bind(NasDto::getPorts, NasDto::setPorts);
            binder.forField(secret)
                    .asRequired("Secret is required")
                    .withValidator(new StringLengthValidator("Secret can contain " +
                            "at most 60 characters.", 0, 60))
                    .bind(NasDto::getSecret, NasDto::setSecret);
            binder.forField(server)
                    .withValidator(new StringLengthValidator("Server can contain " +
                            "at most 64 characters.", 0, 64))
                    .bind(NasDto::getServer, NasDto::setServer);
            binder.forField(community)
                    .bind(NasDto::getCommunity, NasDto::setCommunity);
            binder.forField(description)
                    .bind(NasDto::getDescription, NasDto::setDescription);
        }

        abstract String getDialogTitle();

        abstract Button getConfirmBtn();

    }

}
