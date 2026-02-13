import React, { useState, useEffect, useMemo } from "react";
import { safe } from "../../api/client";
import { listAIModels } from "../../api/AIModels"; 

import {
  Grid,
  Button,
  Pagination,
  Stack,
  TextField,
  MenuItem,
  InputAdornment,
  CircularProgress,
  Alert,
  IconButton
} from "@mui/material";
import SearchIcon from "@mui/icons-material/Search";

import LLMCard from "../../components/LLMCard"; 

const PAGE_SIZE = 8;

const SORT_OPTIONS = [
  { label: "Name (A → Z)", sortBy: "name", sortDir: "asc" },
  { label: "Name (Z → A)", sortBy: "name", sortDir: "desc" },
  { label: "Created (Oldest)", sortBy: "timestamp", sortDir: "asc" },
  { label: "Created (Newest)", sortBy: "timestamp", sortDir: "desc" }
];

const SEARCH_DELAY = 500;

export default function LLMView() {
  const [items, setItems] = useState([]);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(false);
  const [searchInput, setSearchInput] = useState("");
  const [searchQuery, setSearchQuery] = useState("");
  const [sortValue, setSortValue] = useState(SORT_OPTIONS[3]);
  const [error, setError] = useState(null);

  useEffect(() => {
    const t = setTimeout(() => {
      setSearchQuery(searchInput.trim());
      setPage(1); // reset pagination
    }, SEARCH_DELAY);

    return () => clearTimeout(t);
  }, [searchInput]);

  const load = async () => {
    setLoading(true);
    const params = {
      search: searchQuery || undefined,
      sortBy: sortValue.sortBy,
      sortDir: sortValue.sortDir
    };

    const r = await safe(() => listAIModels(params)); // Ensure correct API call

    console.log("API Response: ", r); // Log the API response

    if (r.ok) {
      console.log(r.data?.content); // Verify the structure of the data
      setItems(r.data?.content || []); // Accessing `content` array from the response
    } else {
      setError("Error loading LLM models.");
      setItems([]);
    }

    setLoading(false);
  };

  useEffect(() => {
    load();
  }, [searchQuery, sortValue]);

  const slice = useMemo(() => {
    console.log("Items for pagination: ", items); // Verifies that `items` has the correct data
    return items.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);
  }, [items, page]);

  return (
    <div style={{ padding: 20 }}>
      <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 3 }}>
        <Button
          variant="contained"
          onClick={() => console.log("Add LLM functionality here")}
          sx={{ fontWeight: 900, padding: "8px 16px" }}
        >
          + LLM Model
        </Button>
      </Stack>

      <Stack direction="row" spacing={2} sx={{ mb: 3 }}>
        <TextField
          fullWidth
          placeholder="Search LLM model..."
          value={searchInput}
          onChange={(e) => setSearchInput(e.target.value)}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon sx={{ opacity: 0.6 }} />
              </InputAdornment>
            ),
            endAdornment: loading ? (
              <InputAdornment position="end">
                <CircularProgress size={18} />
              </InputAdornment>
            ) : null
          }}
        />

        <TextField
          select
          label="Sort by"
          value={SORT_OPTIONS.indexOf(sortValue)}
          onChange={(e) => {
            setSortValue(SORT_OPTIONS[e.target.value]);
            setPage(1);
          }}
          sx={{ minWidth: 200 }}
        >
          {SORT_OPTIONS.map((opt, idx) => (
            <MenuItem key={idx} value={idx}>
              {opt.label}
            </MenuItem>
          ))}
        </TextField>
      </Stack>

      {error && <Alert severity="error" sx={{ mb: 3 }}>{error}</Alert>}

      <Grid container spacing={2} sx={{ mt: 2 }}>
        {slice.map((llm, idx) => (
          <Grid item xs={12} sm={6} md={3} key={idx}>
            <LLMCard llm={llm} />
          </Grid>
        ))}

        {!loading && slice.length === 0 && (
          <Grid item xs={12} sx={{ opacity: 0.7, textAlign: "center", mt: 4 }}>
            No LLM Models found
          </Grid>
        )}
      </Grid>

      {items.length > PAGE_SIZE && (
        <Stack alignItems="center" sx={{ mt: 3 }}>
          <Pagination
            count={Math.ceil(items.length / PAGE_SIZE)}
            page={page}
            onChange={(_, p) => setPage(p)}
            color="primary"
          />
        </Stack>
      )}
    </div>
  );
}
