import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.pdp.PDPModel;
import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.Vehicle;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.core.model.road.RoadUser;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.ModelRenderer;
import com.github.rinde.rinsim.ui.renderers.UiSchema;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import com.github.rinde.rinsim.ui.renderers.ViewRect;
import com.google.common.base.Optional;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Preconditions.checkState;

/**
 * Created by bavo en michiel
 */
public class TaskRenderer implements ModelRenderer{

    // static final Point AT_SITE_OFFSET = new Point(-12, -13);
    static final float AT_SITE_ROTATION = 0f;
    // static final Point IN_CARGO_OFFSET = new Point(-21, -1);
    static final float IN_CARGO_ROTATION = 20f;
    static final Point LABEL_OFFSET = new Point(-15, -40);

    ImageType img;

    enum ImageType {

        SMALL("/graphics/perspective/deliverypackage2.png", new Point(-12, -13),
                new Point(-21, -1)),

        LARGE("/graphics/perspective/deliverypackage3.png", new Point(-20, -21),
                new Point(-23, -8));

        final String file;
        final Point atSiteOffset;
        final Point inCargoOffset;

        ImageType(String f, Point atSite, Point inCargo) {
            file = f;
            atSiteOffset = atSite;
            inCargoOffset = inCargo;
        }
    }

    Optional<RoadModel> roadModel;
    Optional<PDPModel> pdpModel;
    final UiSchema uiSchema;

    public TaskRenderer(){
        img = ImageType.SMALL;
        roadModel = Optional.absent();
        pdpModel = Optional.absent();
        uiSchema = new UiSchema(false);
        uiSchema.add(Task.class, img.file);
    }

    @Override
    public void renderStatic(GC gc, ViewPort viewPort) {

    }

    @Override
    public void renderDynamic(GC gc, ViewPort viewPort, long l) {
        uiSchema.initialize(gc.getDevice());

        final Collection<Parcel> parcels = pdpModel.get().getParcels(
                PDPModel.ParcelState.values());
        final Image image = uiSchema.getImage(Task.class);
        checkState(image != null);

        synchronized (pdpModel.get()) {
            for (final Parcel p : parcels) {
                float rotation = AT_SITE_ROTATION;
                int offsetX = 0;
                int offsetY = 0;
                @Nullable
                Task task = (Task) p;
                final Point pos;
                if (task.isAssigned()) {
                    pos = roadModel.get().getPosition(task.getAgent());
                } else {
                    pos = roadModel.get().getPosition(p);
                }
                final int x = viewPort.toCoordX(pos.x);
                final int y = viewPort.toCoordY(pos.y);
                offsetX = (int) img.atSiteOffset.x + x - image.getBounds().width / 2;
                offsetY = (int) img.atSiteOffset.y + y - image.getBounds().height / 2;
                Set<? extends Parcel> objs = roadModel.get().getObjectsAt(p, p.getClass());
                int nb = objs.size();
                gc.drawText(String.valueOf(nb), offsetX + 40, offsetY, true);
                gc.drawImage(image, offsetX, offsetY);
            }
        }
    }

    @Nullable
    @Override
    public ViewRect getViewRect() {
        return null;
    }

    @Override
    public void registerModelProvider(ModelProvider modelProvider) {
        roadModel = Optional.fromNullable(modelProvider.tryGetModel(RoadModel.class));
        pdpModel = Optional.fromNullable(modelProvider.tryGetModel(PDPModel.class));
    }
}
