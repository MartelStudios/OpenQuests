package com.martelstudios.opendialogues.dialogues;

import com.hypixel.hytale.codec.schema.SchemaContext;
import com.hypixel.hytale.codec.schema.config.Schema;
import com.hypixel.hytale.codec.schema.config.StringSchema;
import com.hypixel.hytale.codec.validation.ValidationResults;
import com.hypixel.hytale.codec.validation.Validator;
import com.hypixel.hytale.server.core.asset.common.CommonAsset;
import com.hypixel.hytale.server.core.asset.common.CommonAssetRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * A portrait is a PNG path under {@code Common/}, or only its file name without extension: the
 * Node Editor's file picker writes bare names, the Asset Editor's image picker full paths.
 */
public final class DialoguePortraits {

    /**
     * Shows the Asset Editor's image picker on the field, and only warns about a missing image so a
     * dialogue still loads without its portrait.
     */
    public static final Validator<String> VALIDATOR = new Validator<>() {
        @Override
        public void accept(@Nullable String portrait, @Nonnull ValidationResults results) {
            if (portrait != null && !portrait.isEmpty() && resolve(portrait) == null) {
                results.warn("No portrait image '" + portrait + "' under Common/");
            }
        }

        @Override
        public void updateSchema(SchemaContext context, @Nonnull Schema target) {
            ((StringSchema) target).setHytaleCommonAsset(new StringSchema.CommonAsset("png", true));
        }
    };

    private DialoguePortraits() {}

    /**
     * Looks a bare name up among every pack's images, preferring icons when several share it.
     *
     * @return the image path under {@code Common/}, or {@code null} when none matches.
     */
    @Nullable
    public static String resolve(@Nonnull String portrait) {
        if (portrait.contains("/")) {
            return portrait;
        }

        String suffix = "/" + portrait + ".png";
        String found = null;
        for (List<CommonAssetRegistry.PackAsset> assets : CommonAssetRegistry.getAllAssets()) {
            CommonAsset asset = assets.getLast().asset();
            String name = asset.getName();
            if (name.endsWith(suffix) || name.equals(portrait + ".png")) {
                if (name.startsWith("Icons/")) {
                    return name;
                }
                if (found == null) {
                    found = name;
                }
            }
        }
        return found;
    }
}
