package mobi.droid.fakeroad.ui.view;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by max on 13.01.14.
 */
public class AutoCompleteAddressTextView extends AutoCompleteTextView{


    public AutoCompleteAddressTextView(final Context context){
        super(context);
        setAdapter(new AddressAdapter(context, android.R.layout.simple_dropdown_item_1line));
    }

    public AutoCompleteAddressTextView(final Context context, final AttributeSet attrs){
        super(context, attrs);
        setAdapter(new AddressAdapter(context, android.R.layout.simple_dropdown_item_1line));
    }

    public AutoCompleteAddressTextView(final Context context, final AttributeSet attrs, final int defStyle){
        super(context, attrs, defStyle);
        setAdapter(new AddressAdapter(context, android.R.layout.simple_dropdown_item_1line));
    }

    private static String makeReadableAddress(Address aAddress){

        StringBuilder sb = new StringBuilder();
        for(int i = 0; i <= aAddress.getMaxAddressLineIndex(); i++){
            if(i > 0){
                sb.append(',');
            }
            String line = aAddress.getAddressLine(i);

            if(line != null){
                sb.append(line);
            }
        }
        return sb.toString();
    }

    @Override
    protected CharSequence convertSelectionToString(final Object selectedItem){
        return makeReadableAddress((Address) selectedItem);
    }

    private static ArrayList<Address> autocomplete(Context aContext, String input){
        Geocoder geocoder = new Geocoder(aContext);
        try{
            return new ArrayList<Address>(geocoder.getFromLocationName(input, 5));
        } catch(IOException e){
            e.printStackTrace();
        }
        return null;
    }

    private static class AddressAdapter extends ArrayAdapter<Address> implements Filterable{

        private final LayoutInflater mInflater;
        private ArrayList<Address> resultList;
        private int mResource;
        private int mFieldId = 0;

        public AddressAdapter(final Context context, final int resource){
            super(context, resource);
            mResource = resource;
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent){
            return createViewFromResource(position, convertView, parent, mResource);
        }
        @Override
        public int getCount() {
            return resultList.size();
        }

        @Override
        public Address getItem(int index) {
            return resultList.get(index);
        }

        private View createViewFromResource(int position, View convertView, ViewGroup parent, int resource){
            View view;
            TextView text;

            if(convertView == null){

                view = mInflater.inflate(resource, parent, false);
            } else{
                view = convertView;
            }

            try{
                if(mFieldId == 0){
                    //  If no custom field is assigned, assume the whole resource is a TextView
                    text = (TextView) view;
                } else{
                    //  Otherwise, find the TextView field within the layout
                    text = (TextView) view.findViewById(mFieldId);
                }
            } catch(ClassCastException e){
                Log.e("ArrayAdapter", "You must supply a resource ID for a TextView");
                throw new IllegalStateException(
                        "ArrayAdapter requires the resource ID to be a TextView", e);
            }

            Address item = getItem(position);
            if(item instanceof CharSequence){
                text.setText((CharSequence) item);
            } else{
                text.setText(makeReadableAddress(item));
            }

            return view;
        }

        @Override
        public Filter getFilter(){
            Filter filter = new Filter(){

                @Override
                protected FilterResults performFiltering(CharSequence constraint){
                    FilterResults filterResults = new FilterResults();
                    if(constraint != null){
                        // Retrieve the autocomplete results.
                        resultList = autocomplete(getContext(), constraint.toString());

                        // Assign the data to the FilterResults
                        filterResults.values = resultList;
                        filterResults.count = resultList.size();
                    }
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results){
                    if(results != null && results.count > 0){
                        notifyDataSetChanged();
                    } else{
                        notifyDataSetInvalidated();
                    }
                }
            };
            return filter;
        }

    }
}
