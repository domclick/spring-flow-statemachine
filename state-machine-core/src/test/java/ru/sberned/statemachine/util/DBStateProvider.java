package ru.sberned.statemachine.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import ru.sberned.statemachine.state.StateChanger;
import ru.sberned.statemachine.state.StateProvider;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by jpatuk on 02/05/2017.
 */
public class DBStateProvider implements StateProvider<Item, CustomState, String>, StateChanger<Item, CustomState> {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Map<Item, CustomState> getItemsState(List<String> ids) {
        List<Map<String, Object>> items = jdbcTemplate.queryForList("SELECT id, state FROM item WHERE id IN (?)",
                ids.stream().collect(Collectors.joining( "," )));
        return items.stream().collect(Collectors.toMap(
                m -> new Item((String) m.get("id"), CustomState.getByName((String) m.get("state"))),
                m -> CustomState.getByName((String) m.get("state"))));
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
    public void moveToState(CustomState state, Item item) {
        jdbcTemplate.update("UPDATE item SET state = ? WHERE id = ?", state.name(), item.getId());
    }

    public void cleanItems() {
        jdbcTemplate.update("DELETE FROM item");
    }

    public static class ItemRowMapper implements RowMapper<Item> {

        @Override
        public Item mapRow(ResultSet rs, int rowNum) throws SQLException {
            CustomState state = CustomState.getByName(rs.getString("state"));
            return new Item(rs.getString("id"), state);
        }
    }
}
