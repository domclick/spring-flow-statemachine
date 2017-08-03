package ru.sberned.statemachine.util;

public interface StateTwo {
    public class StartStateTwo implements StateTwo {
        private int i = 1;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof StartStateTwo)) return false;

            StartStateTwo that = (StartStateTwo) o;

            return i == that.i;
        }

        @Override
        public int hashCode() {
            return i;
        }
    }

    public class MiddleStateTwo implements StateTwo {
        private int i = 2;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof MiddleStateTwo)) return false;

            MiddleStateTwo that = (MiddleStateTwo) o;

            return i == that.i;
        }

        @Override
        public int hashCode() {
            return i;
        }
    }

    public class FinishStateTwo implements StateTwo {
        private int i = 3;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof FinishStateTwo)) return false;

            FinishStateTwo that = (FinishStateTwo) o;

            return i == that.i;
        }

        @Override
        public int hashCode() {
            return i;
        }
    }
}
