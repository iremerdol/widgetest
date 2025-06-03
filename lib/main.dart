import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:flutter/services.dart';
import 'dart:convert';
import 'package:intl/intl.dart'; 

void main() {
  WidgetsFlutterBinding.ensureInitialized();
  runApp(const RootApp());
  initializeWidget();
}

const MethodChannel _channel = MethodChannel('widget_channel');

void initializeWidget() async {
  final taskManager = TaskManager();
  await taskManager.loadTasks();
  try {
    await _channel.invokeMethod('updateWidget');
  } catch (e) {
    print("Error invoking 'updateWidget': $e");
  }
}

class Task {
  String id;
  String title;
  bool isCompleted;
  DateTime createdAt;
  DateTime? deadline; 

  Task({
    required this.title,
    this.id = '',
    this.isCompleted = false,
    DateTime? createdAt,
    this.deadline, 
  }) : createdAt = createdAt ?? DateTime.now() {
    id = id.isEmpty ? DateTime.now().microsecondsSinceEpoch.toString() : id;
  }

  Map<String, dynamic> toJson() => {
    'id': id,
    'title': title,
    'isCompleted': isCompleted,
    'createdAt': createdAt.toIso8601String(),
    'deadline': deadline?.toIso8601String(), 
  };

  factory Task.fromJson(Map<String, dynamic> json) => Task(
    id: json['id'],
    title: json['title'],
    isCompleted: json['isCompleted'],
    createdAt: DateTime.parse(json['createdAt']),
    deadline: json['deadline'] != null ? DateTime.parse(json['deadline']) : null, 
  );
}

class TaskManager {
  List<Task> tasks = [];

  Future<void> loadTasks() async {
    final prefs = await SharedPreferences.getInstance();
    final tasksString = prefs.getString('tasks');
    if (tasksString != null) {
      try {
        final List<dynamic> jsonList = jsonDecode(tasksString);
        tasks = jsonList.map((json) => Task.fromJson(json)).toList();
      } catch (e) {
        print("Error decoding tasks from SharedPreferences: $e");
        tasks = [];
      }
    }
  }

  Future<void> _saveTasks() async {
    final prefs = await SharedPreferences.getInstance();
    final tasksJson = tasks.map((task) => task.toJson()).toList();
    await prefs.setString('tasks', jsonEncode(tasksJson));
    try {
      await _channel.invokeMethod('updateWidget');
    } catch (e) {
      print("Error invoking 'updateWidget' during save: $e");
    }
  }

  Future<void> addTask(Task task) async {
    tasks.add(task);
    await _saveTasks();
  }

  Future<void> updateTaskDetails(Task updatedTask) async {
    final taskIndex = tasks.indexWhere((task) => task.id == updatedTask.id);
    if (taskIndex != -1) {
      tasks[taskIndex] = updatedTask; 
      await _saveTasks();
    }
  }

  Future<void> updateTask(String id, String newTitle) async {
    final taskIndex = tasks.indexWhere((task) => task.id == id);
    if (taskIndex != -1) {
      tasks[taskIndex].title = newTitle;
      await _saveTasks();
    }
  }


  Future<void> toggleTaskCompletion(String id) async {
    final taskIndex = tasks.indexWhere((task) => task.id == id);
    if (taskIndex != -1) {
      tasks[taskIndex].isCompleted = !tasks[taskIndex].isCompleted;
      await _saveTasks();
    }
  }

  Future<void> deleteTask(String id) async {
    tasks.removeWhere((task) => task.id == id);
    await _saveTasks();
  }
}

class RootApp extends StatelessWidget {
  const RootApp({super.key});
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'To-Do List Widget',
      theme: ThemeData(primarySwatch: Colors.blue, useMaterial3: true),
      home: const ToDoAppPage(),
    );
  }
}

class ToDoAppPage extends StatefulWidget {
  const ToDoAppPage({super.key});
  @override
  State<ToDoAppPage> createState() => _ToDoAppPageState();
}

class _ToDoAppPageState extends State<ToDoAppPage> {
  final TaskManager taskManager = TaskManager();
  final TextEditingController _taskController = TextEditingController();
  DateTime? _selectedDeadline; 

  @override
  void initState() {
    super.initState();
    _loadTasks();
  }

  Future<void> _loadTasks() async {
    await taskManager.loadTasks();
    if (mounted) {
      setState(() {});
    }
  }

  Future<void> _pickDeadline(BuildContext context, {DateTime? initialDate, required Function(DateTime?) onSelected}) async {
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: initialDate ?? DateTime.now(),
      firstDate: DateTime.now().subtract(const Duration(days: 365)), // Allow past dates for overdue
      lastDate: DateTime.now().add(const Duration(days: 365 * 5)),
    );
    if (picked != null) {
      onSelected(picked);
    }
  }

  Future<void> _addTask() async {
    if (_taskController.text.trim().isNotEmpty) {
      await taskManager.addTask(Task(
        title: _taskController.text.trim(),
        deadline: _selectedDeadline,
      ));
      _taskController.clear();
      if (mounted) {
        setState(() {
          _selectedDeadline = null; 
        });
      }
    }
  }

  Future<void> _editTask(Task task) async {
    _taskController.text = task.title;
    DateTime? tempDeadline = task.deadline; 

    showDialog(
      context: context,
      builder: (dialogContext) {
        return StatefulBuilder( 
          builder: (stfContext, stfSetState) {
            return AlertDialog(
              title: const Text('Edit Task'),
              content: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    TextField(
                      controller: _taskController,
                      autofocus: true,
                      decoration: const InputDecoration(hintText: 'Enter task title'),
                    ),
                    const SizedBox(height: 16),
                    Row(
                      children: [
                        Expanded(
                          child: TextButton.icon(
                            icon: const Icon(Icons.calendar_today_outlined),
                            label: Text(
                              tempDeadline == null
                                  ? 'Set Deadline'
                                  : 'Deadline: ${DateFormat.yMd().format(tempDeadline!)}',
                              style: TextStyle(color: Theme.of(context).primaryColor),
                            ),
                            onPressed: () async {
                              await _pickDeadline(
                                dialogContext, 
                                initialDate: tempDeadline,
                                onSelected: (pickedDate) {
                                  stfSetState(() { 
                                    tempDeadline = pickedDate;
                                  });
                                }
                              );
                            },
                          ),
                        ),
                        if (tempDeadline != null)
                          IconButton(
                            icon: const Icon(Icons.clear, size: 20),
                            tooltip: 'Clear Deadline',
                            onPressed: () {
                              stfSetState(() {
                                tempDeadline = null;
                              });
                            },
                          )
                      ],
                    ),
                  ],
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () {
                    _taskController.clear();
                    Navigator.pop(dialogContext);
                  },
                  child: const Text('Cancel'),
                ),
                ElevatedButton(
                  onPressed: () async {
                    if (_taskController.text.trim().isNotEmpty) {
                      final updatedTask = Task(
                        id: task.id,
                        title: _taskController.text.trim(),
                        isCompleted: task.isCompleted,
                        createdAt: task.createdAt,
                        deadline: tempDeadline, 
                      );
                      await taskManager.updateTaskDetails(updatedTask);
                      _taskController.clear();
                      if (mounted) {
                         setState(() {});
                      }
                      Navigator.pop(dialogContext);
                    }
                  },
                  child: const Text('Save'),
                ),
              ],
            );
          }
        );
      },
    ).then((_) {
      if (_taskController.text.isNotEmpty && _taskController.text == task.title) {
         _taskController.clear(); 
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('To-Do List'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () async {
               try {
                await _channel.invokeMethod('updateWidget');
              } catch (e) {
                print("Error invoking 'updateWidget': $e");
              }
            },
            tooltip: 'Refresh Widget',
          ),
        ],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column( 
              children: [
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: _taskController,
                        decoration: const InputDecoration(
                          hintText: 'Add a new task',
                          border: OutlineInputBorder(),
                        ),
                        onSubmitted: (_) => _addTask(),
                      ),
                    ),
                    const SizedBox(width: 10),
                    FloatingActionButton(
                      onPressed: _addTask,
                      mini: true,
                      child: const Icon(Icons.add),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                Row(
                  mainAxisAlignment: MainAxisAlignment.start,
                  children: [
                    TextButton.icon(
                      icon: const Icon(Icons.calendar_today_outlined, size: 20),
                      label: Text(
                        _selectedDeadline == null
                            ? 'Set Deadline (Optional)'
                            : 'Deadline: ${DateFormat.yMd().format(_selectedDeadline!)}',
                        style: TextStyle(color: Theme.of(context).primaryColor),
                      ),
                      onPressed: () {
                        _pickDeadline(
                          context,
                          initialDate: _selectedDeadline,
                          onSelected: (pickedDate) {
                            setState(() {
                              _selectedDeadline = pickedDate;
                            });
                          }
                        );
                      },
                    ),
                    if (_selectedDeadline != null)
                      IconButton(
                        icon: const Icon(Icons.clear, size: 20),
                        tooltip: 'Clear Deadline',
                        onPressed: () {
                          setState(() {
                            _selectedDeadline = null;
                          });
                        },
                      )
                  ],
                ),
              ],
            ),
          ),
          Expanded(
            child: taskManager.tasks.isEmpty
                ? const Center(
                    child: Text(
                      'No tasks yet!\nAdd your first task.',
                      textAlign: TextAlign.center,
                      style: TextStyle(fontSize: 18),
                    ),
                  )
                : ListView.builder(
                    itemCount: taskManager.tasks.length,
                    itemBuilder: (context, index) {
                      final task = taskManager.tasks[index];
                      return TaskListItem(
                        task: task,
                        onToggle: () async {
                          await taskManager.toggleTaskCompletion(task.id);
                          if (mounted) {
                            setState(() {});
                          }
                        },
                        onEdit: () => _editTask(task),
                        onDelete: () async {
                          await taskManager.deleteTask(task.id);
                          if (mounted) {
                            setState(() {});
                          }
                        },
                      );
                    },
                  ),
          ),
        ],
      ),
    );
  }
}

class TaskListItem extends StatelessWidget {
  final Task task;
  final VoidCallback onToggle;
  final VoidCallback onEdit;
  final VoidCallback onDelete;

  const TaskListItem({
    super.key,
    required this.task,
    required this.onToggle,
    required this.onEdit,
    required this.onDelete,
  });

  @override
  Widget build(BuildContext context) {
    String? deadlineString;
    Color? deadlineColor = Colors.grey[600];
    if (task.deadline != null) {
      final now = DateTime.now();
      final today = DateTime(now.year, now.month, now.day);
      final deadlineDay = DateTime(task.deadline!.year, task.deadline!.month, task.deadline!.day);

      if (deadlineDay.isAtSameMomentAs(today)) {
        deadlineString = 'Ends Today';
        deadlineColor = Colors.orange[700];
      } else if (deadlineDay.isBefore(today) && !task.isCompleted) {
        deadlineString = 'Overdue: ${DateFormat.yMd().format(task.deadline!)}';
        deadlineColor = Colors.red[700];
      } else {
        deadlineString = 'Deadline: ${DateFormat.yMd().format(task.deadline!)}';
      }
    }

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: ListTile(
        leading: Checkbox(
          value: task.isCompleted,
          onChanged: (_) => onToggle(),
        ),
        title: Text(
          task.title,
          style: TextStyle(
            decoration:
                task.isCompleted
                    ? TextDecoration.lineThrough
                    : TextDecoration.none,
            color: task.isCompleted ? Colors.grey : null,
          ),
        ),
        subtitle: deadlineString != null
            ? Text(
                deadlineString,
                style: TextStyle(fontSize: 12, color: deadlineColor),
              )
            : null,
        trailing: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            IconButton(
              icon: const Icon(Icons.edit, size: 20),
              onPressed: onEdit,
              tooltip: 'Edit Task',
            ),
            IconButton(
              icon: const Icon(Icons.delete, size: 20, color: Colors.red),
              onPressed: onDelete,
              tooltip: 'Delete Task',
            ),
          ],
        ),
      ),
    );
  }
}