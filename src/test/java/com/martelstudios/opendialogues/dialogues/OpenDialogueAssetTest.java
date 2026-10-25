package com.martelstudios.opendialogues.dialogues;

import com.hypixel.hytale.codec.ExtraInfo;
import com.martelstudios.opendialogues.actions.DialogueAction;
import com.martelstudios.opendialogues.actions.impl.open.OpenDialogueAction;
import org.bson.BsonDocument;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenDialogueAssetTest {

    @BeforeAll
    static void registerActions() {
        DialogueAction.CODEC.register(OpenDialogueAction.TYPE, OpenDialogueAction.class, OpenDialogueAction.CODEC);
    }

    @Test
    void namesInlineDialoguesAfterTheirAnswerAndAction() {
        OpenDialogueAsset elder = OpenDialogueAsset.BODY_CODEC.decode(BsonDocument.parse("""
            { "Responses": [
                { "Text": "Bye" },
                { "Text": "More", "Actions": [
                    { "Type": "OpenDialoguesOpen", "Dialogue": "Other" },
                    { "Type": "OpenDialoguesOpen", "Inline": { "Responses": [
                        { "Text": "Deeper", "Actions": [ { "Type": "OpenDialoguesOpen", "Inline": {} } ] } ] } } ] } ] }
            """), ExtraInfo.THREAD_LOCAL.get());
        elder.id = "Elder";

        elder.nameInlineDialogues();

        OpenDialogueAsset child = elder.getResponses()[1].getActions()[1].getInlineDialogues().iterator().next();
        OpenDialogueAsset grandChild = child.getResponses()[0].getActions()[0].getInlineDialogues().iterator().next();
        assertEquals("Elder/2.2", child.getId());
        assertEquals("Elder/2.2/1.1", grandChild.getId());
    }
}
