package com.vanbios.bleadvertiser.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.vanbios.bleadvertiser.R;
import com.vanbios.bleadvertiser.object.Device;

import java.util.ArrayList;

public class DevicesRecyclerAdapter extends RecyclerView.Adapter<DevicesRecyclerAdapter.DeviceViewHolder> {

    private ArrayList<Device> devicesList;
    private Context context;


    public DevicesRecyclerAdapter(ArrayList<Device> list, Context context) {
        devicesList = list;
        this.context = context;
    }

    @Override
    public DeviceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new DeviceViewHolder(LayoutInflater.from(
                parent.getContext()).inflate(R.layout.item_device_list, parent, false));
    }

    @Override
    public void onBindViewHolder(DeviceViewHolder holder, int position) {
        holder.tvText.setText(
                String.format(context.getString(R.string.device_info_placeholder),
                        devicesList.get(position).getDeviceName(),
                        devicesList.get(position).getDeviceAddress()));
    }

    @Override
    public int getItemCount() {
        return devicesList.size();
    }


    static class DeviceViewHolder extends RecyclerView.ViewHolder {
        public TextView tvText;

        public DeviceViewHolder(View view) {
            super(view);
            tvText = (TextView) view.findViewById(R.id.tvItemLogGreen);
        }
    }

}