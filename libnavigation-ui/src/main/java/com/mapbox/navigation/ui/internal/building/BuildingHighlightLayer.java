package com.mapbox.navigation.ui.internal.building;

import android.graphics.Color;

import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.style.expressions.Expression;
import com.mapbox.mapboxsdk.style.layers.FillExtrusionLayer;
import com.mapbox.mapboxsdk.style.layers.FillLayer;
import com.mapbox.mapboxsdk.style.sources.VectorSource;
import com.mapbox.navigation.utils.NavigationException;

import androidx.annotation.NonNull;

import static com.mapbox.mapboxsdk.style.expressions.Expression.all;
import static com.mapbox.mapboxsdk.style.expressions.Expression.distance;
import static com.mapbox.mapboxsdk.style.expressions.Expression.eq;
import static com.mapbox.mapboxsdk.style.expressions.Expression.get;
import static com.mapbox.mapboxsdk.style.expressions.Expression.literal;
import static com.mapbox.mapboxsdk.style.expressions.Expression.lt;
import static com.mapbox.mapboxsdk.style.layers.Property.NONE;
import static com.mapbox.mapboxsdk.style.layers.Property.VISIBLE;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillExtrusionColor;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillExtrusionHeight;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillExtrusionOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillOpacity;
import static com.mapbox.mapboxsdk.style.layers.PropertyFactory.visibility;

/**
 * This layer handles the creation and customization of a {@link FillLayer}
 * to highlight the footprint of an individual building. For now, this layer is only
 * compatible with the Mapbox Streets v8 vector tile source. See
 * [https://docs.mapbox.com/vector-tiles/reference/mapbox-streets-v8/]
 * (https://docs.mapbox.com/vector-tiles/reference/mapbox-streets-v8/) for more information
 * about the Mapbox Streets v8 vector tile source.
 */
public class BuildingHighlightLayer {

  public static final String TAG = "BuildingHighlightLayer";
  public static final String BUILDING_HIGHLIGHTED_FOOTPRINT_LAYER_ID = "building-footprint-layer-id";
  public static final String BUILDING_HIGHLIGHTED_EXTRUSION_LAYER_ID = "building-extrusion-highlighted-layer-id";
  private static final String BUILDING_VECTOR_SOURCE_ID = "building-vector-source-id";
  private static final String BUILDING_LAYER_ID = "building";
  private static final Integer DEFAULT_COLOR = Color.BLUE;
  private static final Float DEFAULT_OPACITY = 1f;
  private static final Float QUERY_DISTANCE_MAX_METERS = 1f;
  private final MapboxMap mapboxMap;
  private LatLng queryLatLng;
  private Integer color;
  private Float opacity;

  public BuildingHighlightLayer(MapboxMap mapboxMap) {
    this.mapboxMap = mapboxMap;
    this.mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        VectorSource buildingVectorSource = style.getSourceAs(BUILDING_VECTOR_SOURCE_ID);
        if (buildingVectorSource == null) {
          addVectorSourceToStyle();
        }
      }
    });
  }

  /**
   * Toggles the visibility of the building footprint highlight layer.
   *
   * @param visible true if the layer should be displayed. False if it should be hidden.
   */
  public void updateFootprintVisibility(final boolean visible) {
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        FillLayer buildingFootprintLayer = style.getLayerAs(BUILDING_HIGHLIGHTED_FOOTPRINT_LAYER_ID);
        if (buildingFootprintLayer == null && visible) {
          if (queryLatLng == null) {
            throw new NavigationException("BuildingHighlightLayer's queryLatLng is null. Set"
                + " the query LatLng before you set the footprint's visibility to true");
          } else {
            addFootprintHighlightFillLayerToMap(queryLatLng);
          }
        } else if (buildingFootprintLayer != null) {
          buildingFootprintLayer.setProperties(visibility(visible ? VISIBLE : NONE));
        }
      }
    });
  }

  /**
   * Toggles the visibility of the highlighted extrusion layer.
   *
   * @param visible true if the layer should be displayed. False if it should be hidden.
   */
  public void updateExtrusionVisibility(final boolean visible) {
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        FillExtrusionLayer buildingExtrusionLayer = style.getLayerAs(BUILDING_HIGHLIGHTED_EXTRUSION_LAYER_ID);
        if (buildingExtrusionLayer == null && visible) {
          if (queryLatLng == null) {
            throw new NavigationException("BuildingHighlightLayer's queryLatLng is null. Set"
                + " the query LatLng before you set the extrusion's visibility to true");
          } else {
            addHighlightExtrusionLayerToMap(queryLatLng);
          }
        } else if (buildingExtrusionLayer != null) {
          buildingExtrusionLayer.setProperties(visibility(visible ? VISIBLE : NONE));
        }
      }
    });
  }

  /**
   * Set the {@link LatLng} location of the building highlight layer. The {@link LatLng} passed
   * through this method is used to see whether its within the footprint of a specific
   * building. If so, that building's footprint is used for a 2D highlighted footprint
   * or a 3D highlighted extrusion.
   *
   * @param queryLatLng the new coordinates to use in querying the building layer
   *                    to get the associated {@link Polygon} to eventually highlight.
   */
  public void setQueryLatLng(final LatLng queryLatLng) {
    this.queryLatLng = queryLatLng;
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        FillLayer buildingFootprintLayer = style.getLayerAs(BUILDING_HIGHLIGHTED_FOOTPRINT_LAYER_ID);
        if (buildingFootprintLayer != null) {
          buildingFootprintLayer.setFilter(getFilterExpression(queryLatLng));
        }

        FillExtrusionLayer buildingExtrusionLayer = style.getLayerAs(BUILDING_HIGHLIGHTED_EXTRUSION_LAYER_ID);
        if (buildingExtrusionLayer != null) {
          buildingExtrusionLayer.setFilter(getFilterExpression(queryLatLng));
        }
      }
    });
  }

  /**
   * Set the color of the building highlight layer.
   *
   * @param newFootprintColor the new color value
   */
  public void setColor(final int newFootprintColor) {
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        FillLayer buildingFootprintFillLayer = style.getLayerAs(BUILDING_HIGHLIGHTED_FOOTPRINT_LAYER_ID);
        if (buildingFootprintFillLayer != null) {
          buildingFootprintFillLayer.withProperties(fillColor(newFootprintColor));
        }
        FillExtrusionLayer buildingFillExtrusionLayer = style.getLayerAs(BUILDING_HIGHLIGHTED_EXTRUSION_LAYER_ID);
        if (buildingFillExtrusionLayer != null) {
          buildingFillExtrusionLayer.withProperties(fillExtrusionColor(newFootprintColor));
        }
        color = newFootprintColor;
      }
    });
  }

  /**
   * Set the opacity of the building highlight layer.
   *
   * @param newFootprintOpacity the new opacity value
   */
  public void setOpacity(final Float newFootprintOpacity) {
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        FillLayer buildingFootprintFillLayer = style.getLayerAs(BUILDING_HIGHLIGHTED_FOOTPRINT_LAYER_ID);
        if (buildingFootprintFillLayer != null) {
          buildingFootprintFillLayer.withProperties(fillOpacity(newFootprintOpacity));
        }
        FillExtrusionLayer buildingFillExtrusionLayer = style.getLayerAs(BUILDING_HIGHLIGHTED_EXTRUSION_LAYER_ID);
        if (buildingFillExtrusionLayer != null) {
          buildingFillExtrusionLayer.withProperties(fillExtrusionOpacity(newFootprintOpacity));
        }
        opacity = newFootprintOpacity;
      }
    });
  }

  /**
   * Retrieve the latest set color of the building highlight layer.
   *
   * @return the color Integer
   */
  public Integer getColor() {
    return color;
  }

  /**
   * Retrieve the latest set opacity of the building highlight layer.
   *
   * @return the opacity Float
   */
  public Float getOpacity() {
    return opacity;
  }

  /**
   * Retrieve the latest set opacity of the building highlight layer.
   *
   * @return the opacity Float
   */
  public LatLng getQueryLatLng() {
    return queryLatLng;
  }

  /**
   * Customize and add a {@link FillLayer} to the map to show a highlighted
   * building footprint.
   */
  private void addFootprintHighlightFillLayerToMap(LatLng queryLatLng) {
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        FillLayer buildingFootprintFillLayer = new FillLayer(
            BUILDING_HIGHLIGHTED_FOOTPRINT_LAYER_ID, BUILDING_VECTOR_SOURCE_ID);
        buildingFootprintFillLayer.setSourceLayer(BUILDING_LAYER_ID);
        buildingFootprintFillLayer.setFilter(getFilterExpression(queryLatLng));
        buildingFootprintFillLayer.withProperties(
            fillColor(color == null ? DEFAULT_COLOR : color),
            fillOpacity(opacity == null ? DEFAULT_OPACITY : opacity)
        );

        if (style.getLayerAs(BUILDING_LAYER_ID) != null) {
          style.addLayerAbove(buildingFootprintFillLayer, BUILDING_LAYER_ID);
        } else {
          style.addLayer(buildingFootprintFillLayer);
        }
      }
    });
  }

  /**
   * Customize and add a {@link FillExtrusionLayer} to the map to show a
   * highlighted building extrusion.
   */
  private void addHighlightExtrusionLayerToMap(LatLng queryLatLng) {
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        FillExtrusionLayer newBuildingFillExtrusionLayer = new FillExtrusionLayer(
            BUILDING_HIGHLIGHTED_EXTRUSION_LAYER_ID, BUILDING_VECTOR_SOURCE_ID);
        newBuildingFillExtrusionLayer.setSourceLayer(BUILDING_LAYER_ID);
        newBuildingFillExtrusionLayer.setFilter(getFilterExpression(queryLatLng));
        newBuildingFillExtrusionLayer.withProperties(
            fillExtrusionColor(color == null ? DEFAULT_COLOR : color),
            fillExtrusionOpacity(opacity == null ? DEFAULT_OPACITY : opacity),
            fillExtrusionHeight(get("height"))
        );
        if (style.getLayerAs(BUILDING_HIGHLIGHTED_FOOTPRINT_LAYER_ID) != null) {
          style.addLayerAbove(newBuildingFillExtrusionLayer, BUILDING_HIGHLIGHTED_FOOTPRINT_LAYER_ID);
        } else {
          style.addLayer(newBuildingFillExtrusionLayer);
        }
      }
    });
  }

  /**
   * Adds the Mapbox Streets {@link VectorSource} to the {@link Style} object.
   */
  private void addVectorSourceToStyle() {
    mapboxMap.getStyle(new Style.OnStyleLoaded() {
      @Override
      public void onStyleLoaded(@NonNull Style style) {
        VectorSource buildingFootprintVectorSource = new VectorSource(
            BUILDING_VECTOR_SOURCE_ID, "mapbox://mapbox.mapbox-streets-v8");
        style.addSource(buildingFootprintVectorSource);
      }
    });
  }

  /**
   * Get the correct {@link Expression#all(Expression...)} expression to show the building
   * extrusion associated with the query {@link LatLng}.
   *
   * @param queryLatLng the {@link LatLng} to use in determining which building is closest to the coordinate.
   * @return an {@link Expression#all(Expression...)} expression
   */
  private Expression getFilterExpression(LatLng queryLatLng) {
    return all(
        eq(get("extrude"), "true"),
        eq(get("type"), "building"),
        eq(get("underground"), "false"),
        lt(distance(Point.fromLngLat(queryLatLng.getLongitude(), queryLatLng.getLatitude())),
            literal(QUERY_DISTANCE_MAX_METERS)));
  }
}
