package com.example.android.sunshine.app;

/**
 * Created by yaj on 1/3/15.
 */

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * A placeholder fragment containing a simple view.
 */
public  class ForecastFragment extends Fragment {

    private ArrayAdapter<String> adapter;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);

    }

    @Override
    public  void onCreateOptionsMenu(Menu menu, MenuInflater inflater){
        inflater.inflate(R.menu.forecastfragment,menu);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        ArrayList<String> myList = new ArrayList<String>();
        final ListView list_forecastview = (ListView) rootView.findViewById(R.id.listview_forecastlistView);

        String[] values = new String [] {"Today is cold ", "Tomorrow is painful", " Wednesday is hot",
                "Thursday is happy", "Friday is joyful"};

        for (int i =0; i < values.length; i++ ) {
            myList.add(values[i]);
        }

         adapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item_forecast,R.id.list_item_forecast_textview,myList);

        list_forecastview.setAdapter(adapter);
        list_forecastview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Context context = getActivity().getApplicationContext();
               // CharSequence text = "Hello toast!";
                CharSequence text = String.valueOf(list_forecastview.getItemAtPosition(position));
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(context, text, duration);
                //toast.show();

                Intent detailActivityIntent = new Intent(getActivity(),DetailActivity.class);
                detailActivityIntent.putExtra(Intent.EXTRA_TEXT,text);
                startActivity(detailActivityIntent);


            }
        });



        return rootView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_refresh){
            FetchWeatherTask fetch_weather;
            Log.i("options_menu","Creating task");
            fetch_weather = new FetchWeatherTask();
            fetch_weather.execute("94043");
        return  false;
        }
        return super.onOptionsItemSelected(item);
    }



    private String parse_date(long time){
        time = time * 1000;
        Date date = new Date(time);
        DateFormat formated_date = new SimpleDateFormat("EEE, MMM ,d");
        return formated_date.format(date).toString();

    }


    private String[] parseJson (String weatherJsonStr)
            throws JSONException {

        String[] forecast = new String[7];
        String date;
        Double maxTemperature,minTemperature;
        long max_temperature,min_temperature;
        long time;
        String condition;

        JSONObject weatherMap = new JSONObject(weatherJsonStr);
        JSONArray list = weatherMap.getJSONArray("list");

        for (int i = 0; i < list.length(); i++) {

            JSONObject day = list.getJSONObject(i);

            time = day.getLong("dt");

            date = parse_date(time);

            JSONObject temperature = day.getJSONObject("temp");
            JSONObject weather = day.getJSONArray("weather").getJSONObject(0);
            condition = weather.getString("main");

            maxTemperature = temperature.getDouble("max");
            minTemperature = temperature.getDouble("min");

            max_temperature = Math.round(maxTemperature);
            min_temperature = Math.round(minTemperature);

            forecast[i] = date + " - " + condition + " - " + max_temperature + "/" + min_temperature;


        }

        //System.out.println(weatherMap.optInt("max"));
        //weatherMap
        return forecast;
        //return maxTemp;
    }


    public class FetchWeatherTask extends AsyncTask<String,Void,String[]> {

        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();


        @Override
        protected String[] doInBackground(String... params) {

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;
            String [] weatherResults = null;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are available at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast

                Uri.Builder builder = new Uri.Builder();
                int numDays = 7;
                builder.scheme("http")
                        .authority("api.openweathermap.org")
                        .appendPath("data")
                        .appendPath("2.5")
                        .appendPath("forecast")
                        .appendPath("daily")
                        .appendQueryParameter("q",params[0])
                        .appendQueryParameter("mode","json")
                        .appendQueryParameter("units","metric")
                        .appendQueryParameter("cnt",Integer.toString(numDays));

                String myurl = builder.build().toString();
                URL url = new URL(myurl);

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                Log.i(LOG_TAG,"About to connect to weather");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();
                //adding code to parse json query
                weatherResults = parseJson(forecastJsonStr);
                //Log.v(LOG_TAG,"Forecast JSON String: " + forecastJsonStr);
                return  weatherResults;

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attempting
                // to parse it.
                forecastJsonStr = null;
            } catch (JSONException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }
            return null;

        }

        @Override
        protected void onPostExecute (String[] result) {

            adapter.clear();

            for(int i =0; i < result.length; i++){

                adapter.add(result[i]);
            }

        }


    }


}