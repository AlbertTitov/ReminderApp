package newfarmstudio.reminderapp.Fragment;


import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import newfarmstudio.reminderapp.Adapter.CurrentTasksAdapter;
import newfarmstudio.reminderapp.Database.DBHelper;
import newfarmstudio.reminderapp.Model.ModelSeparator;
import newfarmstudio.reminderapp.Model.ModelTask;
import newfarmstudio.reminderapp.R;


/**
 * A simple {@link Fragment} subclass.
 */
public class CurrentTaskFragment extends TaskFragment {

    public CurrentTaskFragment() {
        // Required empty public constructor
    }

    OnTaskDoneListener onTaskDoneListener;

    public interface OnTaskDoneListener {
        void onTaskDone(ModelTask task);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            onTaskDoneListener = (OnTaskDoneListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement onTaskDoneListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_current_task, container, false);

        recyclerView = (RecyclerView) rootView.findViewById(R.id.rvCurrentTasks);

        layoutManager = new LinearLayoutManager(getActivity());

        recyclerView.setLayoutManager(layoutManager);

        adapter = new CurrentTasksAdapter(this);

        recyclerView.setAdapter(adapter);

        // Inflate the layout for this fragment
        return rootView;
    }

    @Override
    public void addTask(ModelTask modelTask, boolean saveToDB) {

        int position = -1;
        ModelSeparator separator = null;

        for (int i = 0; i < adapter.getItemCount(); i++) {
            if (adapter.getItem(i).isTask()) {
                ModelTask task = (ModelTask) adapter.getItem(i);
                if (modelTask.getDate() < task.getDate()) {
                    position = i;
                    break;
                }
            }
        }

        if (modelTask.getDate() != 0) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(modelTask.getDate());

            if (calendar.get(Calendar.DAY_OF_YEAR) < Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) {
                modelTask.setDateStatus(ModelSeparator.TYPE_OVERDUE);
                if (!adapter.containsSeparatorOverdue) {
                    adapter.containsSeparatorOverdue = true;
                    separator = new ModelSeparator(ModelSeparator.TYPE_OVERDUE);
                }
            } else if (calendar.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) {
                modelTask.setDateStatus(ModelSeparator.TYPE_TODAY);
                if (!adapter.containsSeparatorToday) {
                    adapter.containsSeparatorToday = true;
                    separator = new ModelSeparator(ModelSeparator.TYPE_TODAY);
                }
            } else if (calendar.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().get(Calendar.DAY_OF_YEAR) + 1) {
                modelTask.setDateStatus(ModelSeparator.TYPE_TOMORROW);
                if (!adapter.containsSeparatorTomorrow) {
                    adapter.containsSeparatorTomorrow = true;
                    separator = new ModelSeparator(ModelSeparator.TYPE_TOMORROW);
                }
            } else if (calendar.get(Calendar.DAY_OF_YEAR) > Calendar.getInstance().get(Calendar.DAY_OF_YEAR) + 1) {
                modelTask.setDateStatus(ModelSeparator.TYPE_FUTURE);
                if (!adapter.containsSeparatorFuture) {
                    adapter.containsSeparatorFuture = true;
                    separator = new ModelSeparator(ModelSeparator.TYPE_FUTURE);
                }
            }

        }

        if (position != -1) {

            if (!adapter.getItem(position - 1).isTask()) {
                if (position - 2 >= 0 && adapter.getItem(position - 2).isTask()) {
                    ModelTask task = (ModelTask) adapter.getItem(position - 2);
                    if (task.getDateStatus() == modelTask.getDateStatus()) {
                        position -= 1;
                    }
                } else if (position - 2 < 0 && modelTask.getDate()==0){
                    position -= 1;
                }
            }

            if (separator != null) {
                adapter.addItem(position - 1, separator);
            }

            adapter.addItem(position, modelTask);
        } else {
            if (separator != null) {
                adapter.addItem(separator);
            }
            adapter.addItem(modelTask);
        }

        if (saveToDB) {

            activity.dbHelper.saveTask(modelTask);
        }
    }

    @Override
    public void findTasks(String title) {

        adapter.removeAllItems();
        List<ModelTask> tasks = new ArrayList<>();

        tasks.addAll(activity.dbHelper.query().getTasks(DBHelper.SELECTION_LIKE_TITLE + " AND " +
                DBHelper.SELECTION_STATUS + " OR "
                + DBHelper.SELECTION_STATUS, new String[]{"%" + title + "%", Integer.toString(ModelTask.STATUS_CURRENT),
                Integer.toString(ModelTask.STATUS_OVERDUE)}, DBHelper.TASK_DATE_COLUMN));

        for (int i = 0; i < tasks.size(); i++) {

            addTask(tasks.get(i), false);
        }
    }

    @Override
    public void addTaskFromDB() {

        adapter.removeAllItems();
        List<ModelTask> tasks = new ArrayList<>();

        tasks.addAll(activity.dbHelper.query().getTasks(DBHelper.SELECTION_STATUS + " OR "
                + DBHelper.SELECTION_STATUS, new String[]{Integer.toString(ModelTask.STATUS_CURRENT),
                Integer.toString(ModelTask.STATUS_OVERDUE)}, DBHelper.TASK_DATE_COLUMN));

        for (int i = 0; i < tasks.size(); i++) {

            addTask(tasks.get(i), false);
        }
    }

    @Override
    public void moveTask(ModelTask task) {
        alarmHelper.removeAlarm(task.getTimeStamp());
        onTaskDoneListener.onTaskDone(task);
    }
}
