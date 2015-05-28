import com.github.rinde.rinsim.core.model.ModelProvider;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.ModelRenderer;
import com.github.rinde.rinsim.ui.renderers.UiSchema;
import com.github.rinde.rinsim.ui.renderers.ViewPort;
import com.github.rinde.rinsim.ui.renderers.ViewRect;
import com.google.common.base.Optional;
import org.eclipse.swt.graphics.GC;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Created by bavo en michiel
 */
public class AgentDataRenderer implements ModelRenderer{

    // static final Point AT_SITE_OFFSET = new Point(-12, -13);
    static final float AT_SITE_ROTATION = 0f;
    // static final Point IN_CARGO_OFFSET = new Point(-21, -1);
    static final float IN_CARGO_ROTATION = 20f;
    static final Point LABEL_OFFSET = new Point(-15, -40);

    Optional<RoadModel> roadModel;
    final UiSchema uiSchema;

    public AgentDataRenderer(){
        roadModel = Optional.absent();
        uiSchema = new UiSchema(false);
    }

    @Override
    public void renderStatic(GC gc, ViewPort viewPort) {

    }

    @Override
    public void renderDynamic(GC gc, ViewPort viewPort, long l) {
        uiSchema.initialize(gc.getDevice());

        final Collection<CNPAgent> agents = ((CNPRoadModel)roadModel.get()).getAgents();

        for (final CNPAgent cnpAgent : agents) {
            float rotation = AT_SITE_ROTATION;
            int offsetX = 0;
            int offsetY = 0;
            @Nullable
            final Point pos = cnpAgent.getPosition().get();
            final int x = viewPort.toCoordX(pos.x);
            final int y = viewPort.toCoordY(pos.y);
            offsetX = x + 0;
            offsetY = y + 10;
            gc.drawText(cnpAgent.getEnergyPercentage()+"%", offsetX, offsetY, true);
            if (cnpAgent.getDestination().isPresent()) {
                offsetX = x + 0;
                offsetY = y - 20;
                gc.drawText(cnpAgent.getDestination().get().toString(), offsetX, offsetY, true);
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
    }
}
