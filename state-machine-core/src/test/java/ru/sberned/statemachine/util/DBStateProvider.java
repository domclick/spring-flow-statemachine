package ru.sberned.statemachine.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.sberned.statemachine.state.ItemWithStateProvider;
import ru.sberned.statemachine.state.StateChanger;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Created by Evgeniya Patuk (jpatuk@gmail.com) on 02/05/2017.
 */
public class DBStateProvider implements ItemWithStateProvider<Item, String>, StateChanger<Item, CustomState> {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Item getItemById(String id) {
        return jdbcTemplate.queryForObject("SELECT id, state FROM item WHERE id = ?", new Object[]{id},
                (resultSet, i) -> new Item(resultSet.getString("id"), CustomState.valueOf(resultSet.getString("state"))));
    }

    public void insertItems(List<Item> items) {
        String query = "INSERT INTO item(id, state) VALUES (?, ?)";
        jdbcTemplate.batchUpdate(query, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int rowNum) throws SQLException {
                ps.setString(1, items.get(rowNum).getId());
                ps.setString(2, items.get(rowNum).getState().name());
            }

            @Override
            public int getBatchSize() {
                return items.size();
            }
        });
    }

    @Override
    public void moveToState(CustomState state, Item item, Object... infos) {
        jdbcTemplate.update("UPDATE item SET state = ? WHERE id = ?", state.name(), item.getId());
    }

    public void cleanItems() {
        jdbcTemplate.update("DELETE FROM item");
    }
}
