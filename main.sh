#!/usr/bin/env bash
src="Geertex--networkanalysis"
out="$HOME/Logs/$src$1.log"
ulimit -s unlimited
printf "" > "$out"

# Download NetworkAnalysis
if [[ "$DOWNLOAD" != "0" ]]; then
  rm -rf $src
  git clone https://github.com/wolfram77/$src
  cd $src
  git checkout for-leiden-communities-openmp
fi

# Build NetworkAnalysis
mkdir -p dist/
mkdir -p build/jar/
./release.sh
jar="dist/$(ls dist/ | head -n 1)"

# Download and build graph-make-undirected
cd build/
rm -rf graph-make-undirected/
git clone https://github.com/ionicf/graph-make-undirected
cd graph-make-undirected/
DOWNLOAD=0 RUN=0 ./main.sh
cd ../..

# Convert graph to binary format, run NetworkAnalysis, and clean up
runNetworkAnalysis() {
  stdbuf --output=L printf "Converting $1 to $1.tsv ...\n"                                | tee -a "$out"
  stdbuf --output=L ./build/graph-make-undirected/a.out -i "$1" -o "$1.tsv" -g "tsv" 2>&1 | tee -a "$out"
  stdbuf --output=L java -jar "$jar" --parallel-workers 64 -q modularity -o "$1.clustering" "$1.tsv" 2>&1 | tee -a "$out"
  stdbuf --output=L printf "\n\n"                                                         | tee -a "$out"
  rm -rf "$1.tsv"
}

# Run NetworkAnalysis on all graphs
runAll() {
  # runNetworkAnalysis "$HOME/Data/web-Stanford.mtx"
  runNetworkAnalysis "$HOME/Data/indochina-2004.mtx"
  runNetworkAnalysis "$HOME/Data/uk-2002.mtx"
  runNetworkAnalysis "$HOME/Data/arabic-2005.mtx"
  runNetworkAnalysis "$HOME/Data/uk-2005.mtx"
  runNetworkAnalysis "$HOME/Data/webbase-2001.mtx"
  runNetworkAnalysis "$HOME/Data/it-2004.mtx"
  runNetworkAnalysis "$HOME/Data/sk-2005.mtx"
  runNetworkAnalysis "$HOME/Data/com-LiveJournal.mtx"
  runNetworkAnalysis "$HOME/Data/com-Orkut.mtx"
  runNetworkAnalysis "$HOME/Data/asia_osm.mtx"
  runNetworkAnalysis "$HOME/Data/europe_osm.mtx"
  runNetworkAnalysis "$HOME/Data/kmer_A2a.mtx"
  runNetworkAnalysis "$HOME/Data/kmer_V1r.mtx"
}

# Run NetworkAnalysis 5 times
for i in {1..5}; do
  runAll
done

# Signal completion
curl -X POST "https://maker.ifttt.com/trigger/puzzlef/with/key/${IFTTT_KEY}?value1=$src$1"
