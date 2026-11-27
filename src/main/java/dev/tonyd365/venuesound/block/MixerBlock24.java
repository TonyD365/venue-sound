package dev.tonyd365.venuesound.block;

import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** VS-24: the larger console, five blocks wide and two deep, same controls with twice the inputs. */
public class MixerBlock24 extends MixerBlock {

    public static final IntegerProperty COL5 = IntegerProperty.create("col", 0, 4);

    public MixerBlock24(Properties properties) {
        super(properties);
    }

    @Override
    public MixerLayout layout() {
        return MixerLayout.VS24;
    }

    @Override
    protected IntegerProperty colProperty() {
        return COL5;
    }
}
