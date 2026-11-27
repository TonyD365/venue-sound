package dev.tonyd365.venuesound.item;

/** Headset or lavalier mic with a beltpack: transmits from anywhere in the inventory, hands free. */
public class BodyPackMicItem extends WirelessMicItem implements net.minecraft.world.item.Equipable {

    private final net.minecraft.world.entity.EquipmentSlot slot;

    /** @param slot where it is worn: the headset on the head, the lavalier clipped to the chest */
    public BodyPackMicItem(net.minecraft.world.entity.EquipmentSlot slot, Properties properties) {
        super(properties);
        this.slot = slot;
    }

    @Override
    public net.minecraft.world.entity.EquipmentSlot getEquipmentSlot() {
        return slot;
    }

    /** Right-click: wear it. Sneak + right-click: change channel; sneak + right-click while worn... see WirelessMicItem. */
    @Override
    public net.minecraft.world.InteractionResultHolder<net.minecraft.world.item.ItemStack> use(net.minecraft.world.level.Level level,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return swapWithEquipmentSlot(this, level, player, hand);
        }
        return super.use(level, player, hand);
    }
}
