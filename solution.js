const fs = require("fs");

// Fraction class for exact arithmetic
class Fraction {
  constructor(numerator, denominator = 1n) {
    this.numerator = BigInt(numerator);
    this.denominator = BigInt(denominator);
    this.reduce();
  }

  reduce() {
    const gcd = this.gcd(this.numerator, this.denominator);
    if (gcd !== 1n) {
      this.numerator /= gcd;
      this.denominator /= gcd;
    }
    if (this.denominator < 0n) {
      this.numerator = -this.numerator;
      this.denominator = -this.denominator;
    }
  }

  gcd(a, b) {
    while (b !== 0n) {
      [a, b] = [b, a % b];
    }
    return a;
  }

  add(other) {
    const newNum =
      this.numerator * other.denominator + other.numerator * this.denominator;
    const newDen = this.denominator * other.denominator;
    return new Fraction(newNum, newDen);
  }

  multiply(other) {
    return new Fraction(
      this.numerator * other.numerator,
      this.denominator * other.denominator
    );
  }

  divide(other) {
    return new Fraction(
      this.numerator * other.denominator,
      this.denominator * other.numerator
    );
  }

  negate() {
    return new Fraction(-this.numerator, this.denominator);
  }

  subtract(other) {
    return this.add(other.negate());
  }

  toString() {
    return `${this.numerator}/${this.denominator}`;
  }

  toBigInt() {
    return this.numerator / this.denominator;
  }
}

// Point class to represent (x, y) coordinates
class Point {
  constructor(x, y) {
    this.x = BigInt(x);
    this.y = BigInt(y);
  }

  toString() {
    return `(${this.x}, ${this.y})`;
  }
}

// Decode a string from given base to BigInt
function decodeFromBase(value, base) {
  const digits = "0123456789abcdefghijklmnopqrstuvwxyz";
  let result = 0n;
  const baseBig = BigInt(base);

  for (let i = 0; i < value.length; i++) {
    const digit = digits.indexOf(value[i].toLowerCase());
    if (digit === -1 || digit >= base) {
      throw new Error(`Invalid digit '${value[i]}' for base ${base}`);
    }
    result = result * baseBig + BigInt(digit);
  }

  return result;
}

// Find the secret C using exact Lagrange interpolation
function findSecretCExact(points, k) {
  if (points.length < k) {
    throw new Error(`Not enough points. Need at least ${k} points.`);
  }

  const interpolationPoints = points.slice(0, k);

  // Use exact fractions for calculations
  let secretC = new Fraction(0, 1);

  for (let i = 0; i < interpolationPoints.length; i++) {
    const p_i = interpolationPoints[i];
    const y_i = new Fraction(p_i.y, 1n);

    // Calculate the Lagrange basis polynomial L_i(0)
    let numerator = new Fraction(1, 1);
    let denominator = new Fraction(1, 1);

    for (let j = 0; j < interpolationPoints.length; j++) {
      if (i !== j) {
        const p_j = interpolationPoints[j];
        const x_i = new Fraction(p_i.x, 1n);
        const x_j = new Fraction(p_j.x, 1n);

        // Numerator: (0 - x_j) = -x_j
        const neg_x_j = x_j.negate();
        numerator = numerator.multiply(neg_x_j);

        // Denominator: (x_i - x_j)
        const diff = x_i.subtract(x_j);
        denominator = denominator.multiply(diff);
      }
    }

    // L_i(0) = numerator / denominator
    const lagrangeBasis = numerator.divide(denominator);

    // Add contribution: y_i * L_i(0)
    secretC = secretC.add(y_i.multiply(lagrangeBasis));
  }

  return secretC.toBigInt();
}

// Main function
function main() {
  try {
    const inputFiles = ["input1.json", "input2.json"];

    for (const inputFile of inputFiles) {
      console.log("=".repeat(50));
      console.log(`Processing: ${inputFile}`);
      console.log("=".repeat(50));

      try {
        const jsonContent = fs.readFileSync(inputFile, "utf8");
        const json = JSON.parse(jsonContent);

        const keys = json.keys;
        const n = parseInt(keys.n);
        const k = parseInt(keys.k);

        console.log(`n: ${n}, k: ${k}`);
        console.log(`Degree of polynomial: ${k - 1}`);
        console.log();

        const points = [];

        for (let i = 1; i <= n; i++) {
          const key = String(i);
          if (json[key]) {
            const root = json[key];
            const x = i;
            const baseStr = root.base;
            const valueStr = root.value;

            const base = parseInt(baseStr);
            const y = decodeFromBase(valueStr, base);

            points.push(new Point(x, y));
            console.log(
              `Point ${i}: x=${x}, y=${y} (decoded from base ${base})`
            );
          }
        }

        console.log();

        // Find the secret C using polynomial interpolation
        const secretC = findSecretCExact(points, k);
        console.log(`Secret C: ${secretC}`);

        // Verify the result
        console.log("\nVerification:");
        for (let i = 0; i < Math.min(k, points.length); i++) {
          const p = points[i];
          console.log(`P(${p.x}) = ${p.y}`);
        }
      } catch (error) {
        console.error(`Error processing ${inputFile}: ${error.message}`);
        console.error(error.stack);
      }
      console.log();
    }
  } catch (error) {
    console.error("Error:", error.message);
    console.error(error.stack);
  }
}

// Run the solution
main();
