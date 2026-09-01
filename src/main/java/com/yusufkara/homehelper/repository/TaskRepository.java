package com.yusufkara.homehelper.repository;

import com.yusufkara.homehelper.model.HomeTask;

import java.io.IOException;
import java.util.List;

public interface TaskRepository {
    List<HomeTask> load() throws IOException;

    void save(List<HomeTask> tasks) throws IOException;
}

