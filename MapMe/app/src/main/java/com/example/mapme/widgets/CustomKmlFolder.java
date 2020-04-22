package com.example.mapme.widgets;

import android.app.Activity;

import com.google.gson.JsonObject;

import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.bonuspack.kml.KmlFeature;
import org.osmdroid.bonuspack.kml.KmlFolder;
import org.osmdroid.bonuspack.kml.Style;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.Overlay;

public class CustomKmlFolder extends KmlFolder {

    /**
     * Build a FolderOverlay, containing (recursively) overlays from all items of this Folder.
     * @param map
     * @param defaultStyle to apply when an item has no Style defined.
     * @param styler to apply
     * @param kmlDocument for Styles
     * @return the FolderOverlay built
     */
    @Override public CustomOverlay buildOverlay(MapView map, Style defaultStyle, Styler styler, KmlDocument kmlDocument){
        CustomOverlay folderOverlay = new CustomOverlay();
        folderOverlay.setName(mName);
        folderOverlay.setDescription(mDescription);
        for (KmlFeature k:mItems){
            Overlay overlay = k.buildOverlay(map, defaultStyle, styler, kmlDocument);
            if (overlay != null)
                folderOverlay.add(overlay);
        }
        if (styler == null)
            folderOverlay.setEnabled(mVisibility);
        else
            styler.onFeature(folderOverlay, this);
        return folderOverlay;
    }
}