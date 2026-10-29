package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Employee
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees ORDER BY name ASC")
    fun getAllEmployees(): Flow<List<Employee>>

    @Query("SELECT * FROM employees WHERE id = :id LIMIT 1")
    suspend fun getEmployeeById(id: String): Employee?

    @Query("SELECT * FROM employees WHERE id = :id LIMIT 1")
    fun observeEmployee(id: String): Flow<Employee?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(employees: List<Employee>)

    @Update
    suspend fun update(employee: Employee)

    @Query("UPDATE employees SET usedLeaveQuota = usedLeaveQuota + :days WHERE id = :employeeId")
    suspend fun incrementUsedLeave(employeeId: String, days: Int)

    @Query("DELETE FROM employees")
    suspend fun clearAll()
}
