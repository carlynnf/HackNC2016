package org.dismap.dismap;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.esri.android.map.Callout;
import com.esri.android.map.FeatureLayer;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.Layer;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.android.map.ags.ArcGISTiledMapServiceLayer;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.android.map.event.OnStatusChangedListener;
import com.esri.core.geodatabase.GeodatabaseFeatureServiceTable;
import com.esri.core.geometry.Envelope;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.map.Feature;
import com.esri.core.map.FeatureResult;
import com.esri.core.map.Graphic;
import com.esri.core.renderer.ClassBreaksRenderer;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleMarkerSymbol;
import com.esri.core.symbol.TextSymbol;
import com.esri.core.tasks.geocode.Locator;
import com.esri.core.tasks.geocode.LocatorFindParameters;
import com.esri.core.tasks.geocode.LocatorGeocodeResult;
import com.esri.core.tasks.query.QueryParameters;
import com.esri.core.tasks.query.QueryTask;

import java.util.List;

public class MainActivity extends AppCompatActivity {


    //Making variables needed for map global
    MapView mMapView;
    GraphicsLayer mLocationLayer;
    Point mLocationLayerPoint;
    String mLocationLayerPointString;
    Boolean mIsMapLoaded;

    //For popup
    View globalCalloutView;
    private Graphic mIdentifiedGraphic;
    private ArcGISFeatureLayer mFeatureLayer;
    GraphicsLayer mGraphicsLayer;
    private String mFeatureServiceURL;
    Callout mapCallout;
    ProgressDialog progress;


    //Variables needed for search
    EditText mSearchEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Instantiating variables needed for map
        mMapView = (MapView)findViewById(R.id.map);
        mLocationLayer = new GraphicsLayer();
        mIsMapLoaded = false;
        mMapView.enableWrapAround(true);

        //Instantiating variables for popup
        globalCalloutView = View.inflate(this, R.layout.marker_popup_layout, null);


        // create a point marker symbol (red, size 10, of type circle)
        SimpleMarkerSymbol simpleMarker = new SimpleMarkerSymbol(Color.BLUE, 10, SimpleMarkerSymbol.STYLE.CIRCLE);
        PictureMarkerSymbol pictureMarker = new PictureMarkerSymbol(ResourcesCompat.getDrawable(getResources(), R.drawable.candy, null));

        // create a point at x=-302557, y=7570663 (for a map using meters as units; this depends  // on the spatial reference)
        Point pointGeometry3 = new Point(-302557, 7570663); //uk
        Point pointGeometry4 = new Point(5051950.5, 3696630.3);
        Point pointGeometry5 = GeometryEngine.project(35.905208, -79.050381, mMapView.getSpatialReference());
        Point pointGeometry6 = new Point(-8800517.6, 3970832.0);


        // Get the feature service URL from values->strings.xml
        mFeatureServiceURL = this.getResources().getString(R.string.data);
        // Add Feature layer to the MapView
        mFeatureLayer = new ArcGISFeatureLayer(mFeatureServiceURL, ArcGISFeatureLayer.MODE.ONDEMAND);
        mMapView.addLayer(mFeatureLayer);
        // Add Graphics layer to the MapView
        mGraphicsLayer = new GraphicsLayer();
        mMapView.addLayer(mGraphicsLayer);
        new QueryFeatureLayer().execute();

        //Setting an onStatuser listener for the map
        mMapView.setOnStatusChangedListener(new OnStatusChangedListener() {
            public void onStatusChanged(Object source, STATUS status) {
                if ((source == mMapView) && (status == STATUS.INITIALIZED)) {
                    mIsMapLoaded = true;
                }
            }
        });

        // Identify on single tap of map
        mMapView.setOnSingleTapListener(new OnSingleTapListener() {

            @Override
            public void onSingleTap(final float x, final float y) {

                if (mIsMapLoaded) {
                    // If map is initialized and Single tap is registered on screen
                    // identify the location selected
                    identifyLocation(x, y);
                }
                }

        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    /**
     * Takes in the screen location of the point to identify the feature on map.
     *
     * @param x
     *          x co-ordinate of point
     * @param y
     *          y co-ordinate of point
     */
    void identifyLocation(float x, float y) {

        // Hide the callout, if the callout from previous tap is still showing
        // on map
        if (mapCallout.isShowing()) {
            mapCallout.hide();
        }

        // Find out if the user tapped on a feature
        SearchForFeature(x, y);

        // If the user tapped on a feature, then display information regarding
        // the feature in the callout
        if (mIdentifiedGraphic != null) {
            Point mapPoint = mMapView.toMapPoint(x, y);
            // Show Callout
            ShowCallout(mapCallout, mIdentifiedGraphic, mapPoint);
        }
    }

    /**
     * Sets the value of mIdentifiedGraphic to the Graphic present on the
     * location of screen tap
     *
     * @param x
     *          x co-ordinate of point
     * @param y
     *          y co-ordinate of point
     */
    private void SearchForFeature(float x, float y) {

        Point mapPoint = mMapView.toMapPoint(x, y);

        if (mapPoint != null) {

            for (Layer layer : mMapView.getLayers()) {
                if (layer == null)
                    continue;

                if (layer instanceof ArcGISFeatureLayer) {
                    ArcGISFeatureLayer fLayer = (ArcGISFeatureLayer) layer;
                    // Get the Graphic at location x,y
                    mIdentifiedGraphic = GetFeature(fLayer, x, y);
                }
            }
        }
    }

    /**
     * Returns the Graphic present the location of screen tap
     *
     * @param fLayer
     *          ArcGISFeatureLayer to get graphics ids
     * @param x
     *          x co-ordinate of point
     * @param y
     *          y co-ordinate of point
     * @return Graphic at location x,y
     */
    private Graphic GetFeature(ArcGISFeatureLayer fLayer, float x, float y) {

        // Get the graphics near the Point.
        int[] ids = fLayer.getGraphicIDs(x, y, 10, 1);
        if (ids == null || ids.length == 0) {
            return null;
        }
        return fLayer.getGraphic(ids[0]);
    }

    /**
     * Shows the Attribute values for the Graphic in the Callout
     *
     * @param calloutView a callout to show
     * @param graphic selected graphic
     * @param mapPoint point to show callout on map
     */
    private void ShowCallout(Callout calloutView, Graphic graphic, Point mapPoint) {

        // Create callout from mapview
        mapCallout = mMapView.getCallout();
        mapCallout.setCoordinates(mapPoint);
        mapCallout.setOffset(0, -3);
        mapCallout.setContent(globalCalloutView);
        // show callout
        mapCallout.show();

        /*// Get the values of attributes for the Graphic
        String cityName = (String) graphic.getAttributeValue("NAME");
        String countryName = (String) graphic.getAttributeValue("COUNTRY");
        String cityPopulationValue = graphic.getAttributeValue("POPULATION").toString();

        // Set callout properties
        calloutView.setCoordinates(mapPoint);
        calloutView.setStyle(mCalloutStyle);
        calloutView.setMaxWidth(325);

        // Compose the string to display the results
        StringBuilder cityCountryName = new StringBuilder();
        cityCountryName.append(cityName);
        cityCountryName.append(", ");
        cityCountryName.append(countryName);

        TextView calloutTextLine1 = (TextView) findViewById(R.id.citycountry);
        calloutTextLine1.setText(cityCountryName);

        // Compose the string to display the results
        StringBuilder cityPopulation = new StringBuilder();
        cityPopulation.append("Population: ");
        cityPopulation.append(cityPopulationValue);

        TextView calloutTextLine2 = (TextView) findViewById(R.id.population);
        calloutTextLine2.setText(cityPopulation);
        calloutView.setContent(mCalloutContent);
        calloutView.show();*/
    }

    public void onSearchButtonClicked(View view){
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        String address = mSearchEditText.getText().toString();
        executeLocatorTask(address);
    }

    private void executeLocatorTask(String address) {
        // Create Locator parameters from single line address string
        LocatorFindParameters findParams = new LocatorFindParameters(address);

        // Use the centre of the current map extent as the find location point
        findParams.setLocation(mMapView.getCenter(), mMapView.getSpatialReference());

        // Calculate distance for find operation
        Envelope mapExtent = new Envelope();
        mMapView.getExtent().queryEnvelope(mapExtent);
        // assume map is in metres, other units wont work, double current envelope
        double distance = (mapExtent != null && mapExtent.getWidth() > 0) ? mapExtent.getWidth() * 2 : 10000;
        findParams.setDistance(distance);
        findParams.setMaxLocations(2);

        // Set address spatial reference to match map
        findParams.setOutSR(mMapView.getSpatialReference());

        // Execute async task to find the address
        new LocatorAsyncTask().execute(findParams);
        mLocationLayerPointString = address;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        View searchRef = menu.findItem(R.id.action_search).getActionView();
        mSearchEditText = (EditText) searchRef.findViewById(R.id.searchText);

        mSearchEditText.setOnKeyListener(new View.OnKeyListener() {

            @Override
            public boolean onKey(View view, int keyCode, KeyEvent keyEvent) {
                if(keyCode == KeyEvent.KEYCODE_ENTER){
                    onSearchButtonClicked(mSearchEditText);
                    return true;
                }
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private class LocatorAsyncTask extends AsyncTask<LocatorFindParameters, Void, List<LocatorGeocodeResult>> {
        private Exception mException;

        @Override
        protected List<LocatorGeocodeResult> doInBackground(LocatorFindParameters... params) {
            mException = null;
            List<LocatorGeocodeResult> results = null;
            Locator locator = Locator.createOnlineLocator();
            try {
                results = locator.find(params[0]);
            } catch (Exception e) {
                mException = e;
            }
            return results;
        }

        protected void onPostExecute(List<LocatorGeocodeResult> result) {
            if (mException != null) {
                Log.w("DisMap Search", "LocatorSyncTask failed with:");
                mException.printStackTrace();
                Toast.makeText(MainActivity.this, getString(R.string.addressSearchFailed), Toast.LENGTH_LONG).show();
                return;
            }

            if (result.size() == 0) {
                Toast.makeText(MainActivity.this, getString(R.string.noResultsFound), Toast.LENGTH_LONG).show();
            } else {
                // Use first result in the list
                LocatorGeocodeResult geocodeResult = result.get(0);

                // get return geometry from geocode result
                Point resultPoint = geocodeResult.getLocation();
                // create marker symbol to represent location
                SimpleMarkerSymbol resultSymbol = new SimpleMarkerSymbol(Color.RED, 16, SimpleMarkerSymbol.STYLE.CROSS);
                // create graphic object for resulting location
                Graphic resultLocGraphic = new Graphic(resultPoint, resultSymbol);
                // add graphic to location layer
                mLocationLayer.addGraphic(resultLocGraphic);

                // create text symbol for return address
                String address = geocodeResult.getAddress();
                TextSymbol resultAddress = new TextSymbol(20, address, Color.BLACK);
                // create offset for text
                resultAddress.setOffsetX(-4 * address.length());
                resultAddress.setOffsetY(10);
                // create a graphic object for address text
                Graphic resultText = new Graphic(resultPoint, resultAddress);
                // add address text graphic to location graphics layer
                mLocationLayer.addGraphic(resultText);

                mLocationLayerPoint = resultPoint;

                // Zoom map to geocode result location
                mMapView.zoomToResolution(geocodeResult.getLocation(), 2);
            }
        }
    }

    /**
     * Run the query task on the feature layer and put the result on the map.
     */
    private class QueryFeatureLayer extends AsyncTask<Void, Void, FeatureResult> {

        // default constructor
        public QueryFeatureLayer() {
        }

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(MainActivity.this, "", "Please wait....query task is executing");
        }

        @Override
        protected FeatureResult doInBackground(Void... v) {

            // Define a new query and set parameters
            QueryParameters mParams = new QueryParameters();
            mParams.setReturnGeometry(true);

            // Define the new instance of QueryTask
            QueryTask queryTask = new QueryTask(mFeatureServiceURL);
            FeatureResult results;

            try {
                // run the querytask
                results = queryTask.execute(mParams);
                return results;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(FeatureResult results) {

            // Remove the result from previously run query task
            mGraphicsLayer.removeAll();

            // Define a new marker symbol for the result graphics
            SimpleMarkerSymbol sms = new SimpleMarkerSymbol(Color.BLUE, 10, SimpleMarkerSymbol.STYLE.CIRCLE);

            // Envelope to focus on the map extent on the results
            Envelope extent = new Envelope();

            // iterate through results
            for (Object element : results) {
                // if object is feature cast to feature
                if (element instanceof Feature) {
                    Feature feature = (Feature) element;
                    // convert feature to graphic
                    Graphic graphic = new Graphic(feature.getGeometry(), sms, feature.getAttributes());
                    // merge extent with point
                    extent.merge((Point)graphic.getGeometry());
                    // add it to the layer
                    mGraphicsLayer.addGraphic(graphic);
                }
            }

            // Set the map extent to the envelope containing the result graphics
            mMapView.setExtent(extent, 100);
            // Disable the progress dialog
            progress.dismiss();

        }
    }

}
