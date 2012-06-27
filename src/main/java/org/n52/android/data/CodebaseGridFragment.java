package org.n52.android.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.n52.android.data.CodebaseDownloader.CodebaseHolder;
import org.n52.android.data.PluginLoader.PluginHolder;
import org.n52.android.data.PluginLoader.PluginUpdateListener;
import org.n52.android.geoar.R;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class CodebaseGridFragment extends Fragment {

	private class ProgressUpdater extends ResultReceiver {

		public ProgressUpdater(Handler handler) {
			super(handler);
		}

		@Override
		protected void onReceiveResult(int resultCode, Bundle resultData) {
			super.onReceiveResult(resultCode, resultData);
			if (resultCode == CodebaseDownloadService.PROGRESS_CB_UPDATE) {
				int progressUpdate = resultData.getInt("progress_update");
				progressDialog.setProgress(progressUpdate);
				if (progressUpdate == 100)
					progressDialog.dismiss();
			}
		}
	}

	class GridChangedCallback implements Serializable {
		private static final long serialVersionUID = 147824918094947552L;

		void addDatasourceMap() {
			gridAdapter.notifyDataSetChanged();
		}

		void setProgressUpdater() {
			// initialize progress dialog for downloading progress
			progressDialog = new ProgressDialog(getActivity());
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setTitle("Please Wait");
			progressDialog.setMessage("Downloading Datasource...");
			progressDialog.show();

			// fire the downloader with a new ProgressUpdater reference
			Intent downloadIntent = new Intent(getActivity(),
					CodebaseDownloadService.class);
			downloadIntent.putExtra("url", selectedDatasource.downloadLink);
			downloadIntent.putExtra("resultReceiver", new ProgressUpdater(
					new Handler()));
			getActivity().startService(downloadIntent);
		}
	}

	private GridView 		gridView;
	private GridAdapter 	gridAdapter;
	private ProgressDialog 	progressDialog;
	private CodebaseHolder 	selectedDatasource;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.cb_grid_fragment, container,
				false);
		gridView = (GridView) view.findViewById(R.id.cb_grid_view);
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if (getActivity() != null) {
			gridAdapter = new GridAdapter(getActivity());
			// gridAdapter.init();

			if (gridView != null) {
				gridView.setAdapter(gridAdapter);
			}

			gridView.setOnItemClickListener(new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					// show DialogFragment for the chosen DataSource
					if (getActivity() != null) {
						selectedDatasource = (CodebaseHolder) gridAdapter
								.getItem(position);

						// set dialog params and show dialog
						DatasourceDialogFragment dialogFragment = DatasourceDialogFragment
								.newInstance(selectedDatasource.identification,
										selectedDatasource.name,
										selectedDatasource.description, false,
										new GridChangedCallback());
						dialogFragment.show(getFragmentManager(), "Datasource");

						// TODO delete this! toast not needed
						Toast.makeText(getActivity(),
								selectedDatasource.identification,
								Toast.LENGTH_SHORT).show();
					}

				}
			});
		}
	}

	private class GridAdapter extends BaseAdapter implements
			PluginUpdateListener {

		private class ViewHolder {
			public ImageView imageView;
			public TextView textView;
		}

		private List<CodebaseHolder> codebase;

		private LayoutInflater inflater;

		public GridAdapter(Context context) {
			inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			codebase = new ArrayList<CodebaseHolder>();
			PluginLoader.addPluginUpdateListener(this, CodebaseHolder.class);
		}

		@Override
		public int getCount() {
			if (codebase != null)
				return codebase.size();
			return 0;
		}

		@Override
		public Object getItem(int position) {
			if (codebase != null && position < getCount() && position >= 0)
				return codebase.toArray()[position];
			return null;
		}

		@Override
		public long getItemId(int position) {
			// if(dataSources != null && position < getCount() && position >= 0)
			// return dataSources[position;

			return 0;
		}

		@Override
		public View getView(int position, View cView, ViewGroup parent) {
			View view = cView;
			ViewHolder viewHolder;

			if (view == null) {
				view = inflater.inflate(R.layout.cb_grid_item, parent, false);
				viewHolder = new ViewHolder();
				viewHolder.imageView = (ImageView) view
						.findViewById(R.id.cb_grid_image);
				viewHolder.textView = (TextView) view
						.findViewById(R.id.cb_grid_label);

				view.setTag(viewHolder);
			} else {
				viewHolder = (ViewHolder) view.getTag();
			}

			CodebaseHolder cur = codebase.get(position);
			ImageLoader.getInstance().displayImage(cur.identification,
					cur.imageLink, viewHolder.imageView);
			// request Async datasource image download TODO delete
			viewHolder.textView.setText(cur.identification);

			return view;
		}

		@Override
		public void pluginUpdate(PluginHolder holder) {
			this.codebase.add((CodebaseHolder) holder);
			notifyDataSetChanged();
		}

		@Override
		public void refreshViewOnMainThread() {
			notifyDataSetChanged();
		}
	}
}