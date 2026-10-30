package com.martelstudios.opendialogues.actions.impl.inventory;

import com.martelstudios.opendialogues.CodecJson;
import com.martelstudios.opendialogues.conditions.impl.items.OpenDialoguesItemFilter;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OpenDialoguesConsumeInventoryActionTest {

    @Test
    void quantitySpansStacks() {
        int[] removals = OpenDialoguesConsumeInventoryAction.removals(new String[]{"Ore_Copper", "Ore_Copper"}, new int[]{7, 5}, List.of(need("Ore_Copper", 10)));

        assertArrayEquals(new int[]{7, 3}, removals);
    }

    @Test
    void shortEntryRemovesNothing() {
        int[] removals = OpenDialoguesConsumeInventoryAction.removals(new String[]{"Ore_Copper", "Ore_Gold"}, new int[]{10, 4}, List.of(need("Ore_Copper", 10), need("Ore_Gold", 10)));

        assertNull(removals);
    }

    @Test
    void overlappingEntriesShareSlots() {
        int[] removals = OpenDialoguesConsumeInventoryAction.removals(new String[]{"Ore_Copper"}, new int[]{10}, List.of(need("Ore_Copper", 10), need("Ore_Copper", 10)));

        assertNull(removals);
    }

    @Test
    void emptySlotsAreSkipped() {
        int[] removals = OpenDialoguesConsumeInventoryAction.removals(new String[]{null, "Ore_Gold"}, new int[]{0, 10}, List.of(need("Ore_Gold", 10)));

        assertArrayEquals(new int[]{0, 10}, removals);
    }

    @Test
    void zeroQuantityIsRefused() {
        assertNull(CodecJson.decode(OpenDialoguesConsumeInventoryAction.CODEC, "{ \"Items\": [ { \"Item\": { \"BlockTag\": \"Ore\" }, \"Quantity\": 0 } ] }"));
    }

    @Test
    void noItemsIsRefused() {
        assertNull(CodecJson.decode(OpenDialoguesConsumeInventoryAction.CODEC, "{ \"Items\": [] }"));
    }

    @Nonnull
    private static OpenDialoguesItemQuantity need(@Nonnull String wanted, int quantity) {
        OpenDialoguesItemFilter filter = new OpenDialoguesItemFilter() {
            @Override
            public boolean matches(@Nullable String id) {
                return wanted.equals(id);
            }
        };
        return new OpenDialoguesItemQuantity(filter, quantity);
    }
}
